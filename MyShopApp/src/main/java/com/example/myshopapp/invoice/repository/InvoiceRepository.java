package com.example.myshopapp.invoice.repository;

import com.example.myshopapp.invoice.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByOrderId(Long orderId);

    Optional<Invoice> findByOrderId(Long orderId);

    Page<Invoice> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    Page<Invoice> findAllByOrderByIdDesc(Pageable pageable);

    Page<Invoice> findByStatusOrderByIdDesc(String status, Pageable pageable);
}
