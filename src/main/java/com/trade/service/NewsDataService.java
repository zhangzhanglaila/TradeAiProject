package com.trade.service;

import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvRowHandler;
import cn.hutool.core.text.csv.CsvUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.common.BusinessException;
import com.trade.common.ErrorCode;
import com.trade.entity.NewsData;
import com.trade.mapper.NewsDataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class NewsDataService {
    private static final Logger log = LoggerFactory.getLogger(NewsDataService.class);

    @Autowired
    private NewsDataMapper newsDataMapper;

    public Page<NewsData> getPage(Integer current, Integer size, String keyword, String startDate, String endDate) {
        Page<NewsData> page = new Page<>(current, size);
        LambdaQueryWrapper<NewsData> wrapper = buildQueryWrapper(keyword, startDate, endDate);
        wrapper.orderByDesc(NewsData::getCreateTime);
        return newsDataMapper.selectPage(page, wrapper);
    }

    public Page<NewsData> getPage(Integer current, Integer size, String keyword) {
        return getPage(current, size, keyword, null, null);
    }

    public List<NewsData> listForExport(String keyword) {
        LambdaQueryWrapper<NewsData> wrapper = buildQueryWrapper(keyword, null, null);
        wrapper.orderByAsc(NewsData::getId);
        return newsDataMapper.selectList(wrapper);
    }

    private LambdaQueryWrapper<NewsData> buildQueryWrapper(String keyword, String startDate, String endDate) {
        LambdaQueryWrapper<NewsData> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(NewsData::getTitle, keyword);
        }

        // pubDate 为 LocalDateTime，参数按 YYYY-MM-DD 传入
        if (startDate != null && !startDate.isEmpty()) {
            try {
                LocalDate d = LocalDate.parse(startDate);
                wrapper.ge(NewsData::getPubDate, d.atStartOfDay());
            } catch (Exception ignored) {
            }
        }
        if (endDate != null && !endDate.isEmpty()) {
            try {
                LocalDate d = LocalDate.parse(endDate);
                wrapper.le(NewsData::getPubDate, d.atTime(23, 59, 59));
            } catch (Exception ignored) {
            }
        }
        return wrapper;
    }

    public NewsData getById(Long id) {
        return newsDataMapper.selectById(id);
    }

    public void save(NewsData newsData) {
        newsDataMapper.insert(newsData);
    }

    public void update(NewsData newsData) {
        newsDataMapper.updateById(newsData);
    }

    public void delete(Long id) {
        newsDataMapper.deleteById(id);
    }

    /**
     * 上传新闻 CSV：无表头 4 列：source_id、content_text、title、pub_date(yyyy/M/d)
     * 说明：内容字段可能被引号包裹且包含大量换行，因此需开启 quoted multiline。
     */
    public void importCsv(MultipartFile file, Integer maxRows, Integer batchSize, String charset) throws Exception {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "上传文件为空或未选择文件");
        }
        int limit = (maxRows == null || maxRows <= 0) ? Integer.MAX_VALUE : maxRows;
        int batch = (batchSize == null || batchSize <= 0) ? 500 : batchSize;

        Charset cs;
        try {
            cs = (charset == null || charset.isBlank()) ? Charset.forName("GB18030") : Charset.forName(charset);
        } catch (UnsupportedCharsetException e) {
            cs = Charset.forName("GB18030");
        }

        // MultipartFile.getInputStream() 可能不支持 mark/reset，且 CSV 解析可能需要多次读取。
        // 这里先将文件读入内存，再用 ByteArrayInputStream 提供可重复读取的流。
        byte[] bytes;
        try (InputStream in = file.getInputStream(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            bytes = bos.toByteArray();
        }

        log.info("news csv upload received, filename={}, contentType={}, size={}, bytesRead={}",
                file.getOriginalFilename(), file.getContentType(), file.getSize(), bytes.length);

        if (bytes.length == 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "上传文件内容为空（请确认以 multipart/form-data 方式上传，字段名为 file）");
        }

        CsvReadConfig config = new CsvReadConfig();
        config.setContainsHeader(false);
        config.setErrorOnDifferentFieldCount(false);
        // 明确指定逗号分隔（避免某些环境默认值被改动）
        config.setFieldSeparator(',');
        // Hutool 5.8.x 使用 textDelimiter 控制引号包裹字段；没有 setQuote/setEscape
        config.setTextDelimiter('"');
        // 禁用注释行解析，避免以 # 等开头的行被跳过
        config.disableComment();

        CsvReader csvReader = CsvUtil.getReader(config);

        try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(bytes), cs)) {
            List<NewsData> buffer = new ArrayList<>(batch);
            int[] imported = new int[]{0};
            int[] skipped = new int[]{0};

            csvReader.read(reader, new CsvRowHandler() {
                @Override
                public void handle(CsvRow row) {
                    if (imported[0] >= limit) {
                        return;
                    }

                    List<String> raw = row.getRawList();
                    if (raw == null || raw.size() < 4) {
                        skipped[0]++;
                        return;
                    }

                    String sourceId = safeGet(raw, 0);
                    String contentText = safeGet(raw, 1);
                    String title = safeGet(raw, 2);
                    String pubDateStr = safeGet(raw, 3);

                    if ((title == null || title.isBlank()) && contentText != null) {
                        title = extractTitleFromContent(contentText);
                    }

                    // 全部为空/空白的行直接跳过（避免“导入成功但实际 0 行”难排查）
                    boolean allBlank = (isBlank(sourceId) && isBlank(contentText) && isBlank(title) && isBlank(pubDateStr));
                    if (allBlank) {
                        skipped[0]++;
                        return;
                    }

                    LocalDateTime pubDate = parsePubDate(pubDateStr);

                    NewsData news = new NewsData();
                    news.setSourceId(trimToNull(sourceId));
                    news.setContentText(trimToNull(contentText));
                    news.setTitle(trimToNull(title));
                    news.setPubDate(pubDate);
                    news.setDeleted(0);
                    news.setCreateTime(LocalDateTime.now());
                    news.setUpdateTime(LocalDateTime.now());

                    buffer.add(news);
                    imported[0]++;

                    if (buffer.size() >= batch) {
                        newsDataMapper.upsertBatch(buffer);
                        buffer.clear();
                    }
                }
            });

            if (!buffer.isEmpty()) {
                newsDataMapper.upsertBatch(buffer);
            }

            log.info("news csv import done, imported={}, skipped={}, limit={}, batch={}, charset={}, bytes={}",
                    imported[0], skipped[0], limit, batch, cs.name(), bytes.length);

            // 没有任何有效数据时直接报错，让前端/调用方明确知道“实际未入库”
            if (imported[0] == 0) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "CSV 中未解析到有效数据行（可能分隔符/编码/格式不匹配）");
            }
        }
    }

    public void writeExportCsv(OutputStream outputStream, String keyword) throws Exception {
        List<NewsData> list = listForExport(keyword);
        String header = "source_id,content_text,title,pub_date\n";
        outputStream.write(header.getBytes(StandardCharsets.UTF_8));
        for (NewsData n : list) {
            String line = csv(n.getSourceId()) + "," +
                    csv(n.getContentText()) + "," +
                    csv(n.getTitle()) + "," +
                    csv(n.getPubDate()) + "\n";
            outputStream.write(line.getBytes(StandardCharsets.UTF_8));
        }
    }

    private LocalDateTime parsePubDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            // yyyy/M/d
            LocalDate d = LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern("yyyy/M/d"));
            return d.atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractTitleFromContent(String contentText) {
        String[] lines = contentText.split("\\R");
        for (String line : lines) {
            if (line != null) {
                String t = line.trim();
                if (!t.isEmpty()) {
                    if (t.length() > 100) {
                        return t.substring(0, 100);
                    }
                    return t;
                }
            }
        }
        return "";
    }

    private String safeGet(List<String> raw, int idx) {
        if (raw == null || idx < 0 || idx >= raw.size()) {
            return null;
        }
        String v = raw.get(idx);
        return (v == null) ? null : v.trim();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String csv(Object v) {
        String s = (v == null) ? "" : String.valueOf(v);
        s = s.replace("\"", "\"\"");
        return "\"" + s + "\"";
    }
}
