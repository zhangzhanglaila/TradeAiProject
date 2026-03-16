package com.trade.service.kg;

import com.trade.common.KgBuildStatus;
import com.trade.vo.kg.KgBuildProgressVO;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KgBuildProgressStore {

    private final Map<Long, KgBuildProgressVO> store = new ConcurrentHashMap<>();

    public void init(Long historyId) {
        KgBuildProgressVO vo = new KgBuildProgressVO();
        vo.setHistoryId(historyId);
        vo.setStatus(KgBuildStatus.BUILDING.name());
        vo.setProgress(0);
        vo.setMessage("开始构建");
        store.put(historyId, vo);
    }

    public void update(Long historyId, int progress, String message) {
        KgBuildProgressVO vo = store.get(historyId);
        if (vo == null) {
            vo = new KgBuildProgressVO();
            vo.setHistoryId(historyId);
            vo.setStatus(KgBuildStatus.BUILDING.name());
            store.put(historyId, vo);
        }
        vo.setProgress(progress);
        vo.setMessage(message);
    }

    public void succeed(Long historyId) {
        KgBuildProgressVO vo = store.get(historyId);
        if (vo == null) {
            vo = new KgBuildProgressVO();
            vo.setHistoryId(historyId);
            store.put(historyId, vo);
        }
        vo.setStatus(KgBuildStatus.SUCCEEDED.name());
        vo.setProgress(100);
        vo.setMessage("构建完成");
    }

    public void fail(Long historyId, String message) {
        KgBuildProgressVO vo = store.get(historyId);
        if (vo == null) {
            vo = new KgBuildProgressVO();
            vo.setHistoryId(historyId);
            store.put(historyId, vo);
        }
        vo.setStatus(KgBuildStatus.FAILED.name());
        vo.setProgress(Math.min(vo.getProgress() == null ? 0 : vo.getProgress(), 99));
        vo.setMessage(message == null ? "构建失败" : message);
    }

    public KgBuildProgressVO get(Long historyId) {
        return store.get(historyId);
    }
}
