package com.trade.service;

import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvRowHandler;
import cn.hutool.core.text.csv.CsvUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.entity.TradeData;
import com.trade.mapper.TradeDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;

/**
 * 贸易数据服务类
 *
 * 提供贸易数据的增删改查、分页查询和 CSV 导入功能
 */
@Service
public class TradeDataService {

    @Autowired
    private TradeDataMapper tradeDataMapper;

    public Page<TradeData> getPage(Integer current, Integer size, String keyword, String startMonth, String endMonth) {
        Page<TradeData> page = new Page<>(current, size);
        LambdaQueryWrapper<TradeData> wrapper = buildQueryWrapper(keyword, startMonth, endMonth);
        wrapper.orderByDesc(TradeData::getCreateTime);
        return tradeDataMapper.selectPage(page, wrapper);
    }

    public List<TradeData> listForExport(String keyword, String startMonth, String endMonth) {
        LambdaQueryWrapper<TradeData> wrapper = buildQueryWrapper(keyword, startMonth, endMonth);
        wrapper.orderByAsc(TradeData::getId);
        return tradeDataMapper.selectList(wrapper);
    }

    private LambdaQueryWrapper<TradeData> buildQueryWrapper(String keyword, String startMonth, String endMonth) {
        LambdaQueryWrapper<TradeData> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(TradeData::getProductName, keyword)
                    .or().like(TradeData::getPartnerName, keyword));
        }

        if (startMonth != null && !startMonth.isEmpty()) {
            wrapper.ge(TradeData::getDataYearMonth, startMonth);
        }

        if (endMonth != null && !endMonth.isEmpty()) {
            wrapper.le(TradeData::getDataYearMonth, endMonth);
        }

        return wrapper;
    }

    public TradeData getById(Long id) {
        return tradeDataMapper.selectById(id);
    }

    public void save(TradeData tradeData) {
        tradeDataMapper.insert(tradeData);
    }

    public void update(TradeData tradeData) {
        tradeDataMapper.updateById(tradeData);
    }

    public void delete(Long id) {
        tradeDataMapper.deleteById(id);
    }

    public void deleteBatch(List<Long> ids) {
        tradeDataMapper.deleteBatchIds(ids);
    }

    public void importCsv(MultipartFile file) throws Exception {
        importCsv(file, null, null, StandardCharsets.UTF_8.name());
    }

    /**
     * 导入 CSV 文件（逐行读取 + 批量 upsert）
     */
    public void importCsv(MultipartFile file, Integer maxRows, Integer batchSize, String charset) throws Exception {
        int limit = (maxRows == null || maxRows <= 0) ? Integer.MAX_VALUE : maxRows;
        int batch = (batchSize == null || batchSize <= 0) ? 500 : batchSize;

        Charset cs;
        try {
            cs = (charset == null || charset.isBlank()) ? StandardCharsets.UTF_8 : Charset.forName(charset);
        } catch (UnsupportedCharsetException e) {
            cs = StandardCharsets.UTF_8;
        }

        CsvReader csvReader = CsvUtil.getReader();

        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), cs)) {
            List<TradeData> buffer = new ArrayList<>(batch);
            int[] imported = new int[]{0};

            csvReader.read(reader, new CsvRowHandler() {
                @Override
                public void handle(CsvRow row) {
                    // 跳过表头（第一行）
                    if (row.getOriginalLineNumber() == 1) {
                        return;
                    }
                    if (imported[0] >= limit) {
                        return;
                    }

                    List<String> raw = row.getRawList();
                    if (raw == null || raw.size() < 14) {
                        return;
                    }

                    TradeData data = new TradeData();
                    data.setDataYearMonth(safeGet(raw, 0));
                    data.setPartnerCode(safeGet(raw, 1));
                    data.setPartnerName(safeGet(raw, 2));
                    data.setProductCode(safeGet(raw, 3));
                    data.setProductName(safeGet(raw, 4));
                    data.setTradeModeCode(safeGet(raw, 5));
                    data.setTradeModeName(safeGet(raw, 6));
                    data.setRegCode(safeGet(raw, 7));
                    data.setRegName(safeGet(raw, 8));
                    data.setFirstQuantity(parseBigDecimal(safeGet(raw, 9)));
                    data.setFirstUnit(safeGet(raw, 10));
                    data.setSecondQuantity(parseBigDecimal(safeGet(raw, 11)));
                    data.setSecondUnit(safeGet(raw, 12));
                    data.setAmountRmb(parseBigDecimal(safeGet(raw, 13)));
                    if (raw.size() > 14) {
                        data.setRemark(safeGet(raw, 14));
                    }

                    buffer.add(data);
                    imported[0]++;

                    if (buffer.size() >= batch) {
                        tradeDataMapper.insertBatch(buffer);
                        buffer.clear();
                    }
                }
            });

            if (!buffer.isEmpty()) {
                tradeDataMapper.insertBatch(buffer);
            }
        }
    }

    public void writeExportCsv(OutputStream outputStream, String keyword, String startMonth, String endMonth) throws Exception {
        // 为了支持 content/remark 中包含逗号、换行等，这里做最小实现：所有字段用双引号包裹并转义
        List<TradeData> list = listForExport(keyword, startMonth, endMonth);
        String header = "data_year_month,partner_code,partner_name,reg_code,reg_name,product_code,product_name,trade_mode_code,trade_mode_name,first_quantity,first_unit,second_quantity,second_unit,amount_rmb,remark\n";
        outputStream.write(header.getBytes(StandardCharsets.UTF_8));

        for (TradeData t : list) {
            String line = csv(t.getDataYearMonth()) + "," +
                    csv(t.getPartnerCode()) + "," +
                    csv(t.getPartnerName()) + "," +
                    csv(t.getRegCode()) + "," +
                    csv(t.getRegName()) + "," +
                    csv(t.getProductCode()) + "," +
                    csv(t.getProductName()) + "," +
                    csv(t.getTradeModeCode()) + "," +
                    csv(t.getTradeModeName()) + "," +
                    csv(t.getFirstQuantity()) + "," +
                    csv(t.getFirstUnit()) + "," +
                    csv(t.getSecondQuantity()) + "," +
                    csv(t.getSecondUnit()) + "," +
                    csv(t.getAmountRmb()) + "," +
                    csv(t.getRemark()) + "\n";
            outputStream.write(line.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String safeGet(List<String> raw, int idx) {
        if (raw == null || idx < 0 || idx >= raw.size()) {
            return null;
        }
        String v = raw.get(idx);
        return (v == null) ? null : v.trim();
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty() || "?".equals(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String csv(Object v) {
        String s = (v == null) ? "" : String.valueOf(v);
        s = s.replace("\"", "\"\"");
        return "\"" + s + "\"";
    }
}
