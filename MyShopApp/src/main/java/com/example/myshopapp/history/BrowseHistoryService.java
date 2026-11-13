package com.example.myshopapp.history;

import com.example.myshopapp.common.exception.BusinessException;
import com.example.myshopapp.common.exception.ErrorCode;
import com.example.myshopapp.history.dto.BrowseHistoryVO;
import com.example.myshopapp.history.entity.BrowseHistory;
import com.example.myshopapp.history.repository.BrowseHistoryRepository;
import com.example.myshopapp.product.entity.Product;
import com.example.myshopapp.product.repository.ProductRepository;
import com.example.myshopapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 浏览历史：浏览商品时记录，重复浏览刷新时间，支持查看最近记录与清空。
 */
@Service
@RequiredArgsConstructor
public class BrowseHistoryService {

    private final BrowseHistoryRepository historyRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /** 记录一次浏览：同一商品只保留一条记录，重复浏览时更新浏览时间 */
    @Transactional
    public void record(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));

        BrowseHistory history = historyRepository.findByUserIdAndProductId(userId, productId)
                .orElseGet(() -> {
                    BrowseHistory created = new BrowseHistory();
                    created.setUser(userRepository.getReferenceById(userId));
                    created.setProduct(product);
                    return created;
                });
        history.setViewedAt(LocalDateTime.now());
        historyRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<BrowseHistoryVO> recent(Long userId, int limit) {
        int size = Math.min(Math.max(limit, 1), 50);
        return historyRepository.findRecent(userId, PageRequest.of(0, size))
                .stream()
                .map(BrowseHistoryVO::from)
                .toList();
    }

    /** 清空我的浏览历史 */
    @Transactional
    public void clear(Long userId) {
        historyRepository.deleteByUserId(userId);
    }
}
