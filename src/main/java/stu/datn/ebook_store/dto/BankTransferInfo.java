package stu.datn.ebook_store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO chứa thông tin ngân hàng để chuyển khoản
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankTransferInfo {

    /**
     * Tên ngân hàng (VD: TPbank, Vietcombank, ...)
     */
    private String bankName;

    /**
     * Số tài khoản ngân hàng
     */
    private String accountNumber;

    /**
     * Tên chủ tài khoản
     */
    private String accountName;

    /**
     * Chi nhánh ngân hàng
     */
    private String branch;

    /**
     * Mã ngân hàng (Bank code) để tạo QR
     */
    private String bankCode;
}

