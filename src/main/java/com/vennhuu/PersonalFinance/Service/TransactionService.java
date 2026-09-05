package com.vennhuu.PersonalFinance.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.vennhuu.PersonalFinance.Entity.Category;
import com.vennhuu.PersonalFinance.Entity.Request.Transaction.TransactionReq;
import com.vennhuu.PersonalFinance.Entity.Response.File.ResUploadFileDTO;
import com.vennhuu.PersonalFinance.Entity.Response.ResultPaginationDTO;
import com.vennhuu.PersonalFinance.Entity.Response.Transaction.ResTransaction;
import com.vennhuu.PersonalFinance.Entity.Transaction;
import com.vennhuu.PersonalFinance.Entity.User;
import com.vennhuu.PersonalFinance.Entity.Wallet;
import com.vennhuu.PersonalFinance.Enum.TransactionType;
import com.vennhuu.PersonalFinance.Exception.BadRequestException;
import com.vennhuu.PersonalFinance.Exception.ResourceNotFoundException;
import com.vennhuu.PersonalFinance.Exception.StorageException;
import com.vennhuu.PersonalFinance.Repository.CategoryRepository;
import com.vennhuu.PersonalFinance.Repository.TransactionRepository;
import com.vennhuu.PersonalFinance.Repository.WalletRepository;
import com.vennhuu.PersonalFinance.Utils.SecurityUtil;

import jakarta.transaction.Transactional;

@Service
public class TransactionService {

    @Value("${vennhuu.upload-file.base-uri}")
    private String baseURI;
    private static final String RECEIPT_FOLDER = "receipts";
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "jpg", "jpeg", "png");
    private static final long MAX_RECEIPT_SIZE = 5 * 1024 * 1024; // 5MB

    private final TransactionRepository transactionRepository;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;
    private final FileService fileService;

    public TransactionService(
            TransactionRepository transactionRepository,
            SecurityUtil securityUtil,
            UserService userService,
            WalletRepository walletRepository,
            CategoryRepository categoryRepository,
            FileService fileService
    ) {
        this.transactionRepository = transactionRepository;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.walletRepository = walletRepository;
        this.categoryRepository = categoryRepository;
        this.fileService = fileService ;
    }
    

    private User getCurrentUser() {
        String email = securityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BadCredentialsException("Vui lòng đăng nhập"));
        return userService.findByEmail(email);
    }

    private Transaction getOwnedTransaction(Long id, Long userId) {
        return transactionRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch"));
    }

    private Wallet getOwnedWallet(Long walletId, Long userId) {
        return walletRepository.findByIdAndUser_Id(walletId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví"));
    }

    private Category getOwnedCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUser_Id(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
    }

    private void validateCategoryMatchesType(Category category, TransactionType type) {
        if (category.getType() != type) {
            throw new BadRequestException(
                    "Loại giao dịch không khớp với danh mục đã chọn (danh mục này chỉ dùng cho "
                            + category.getType() + ")"
            );
        }
    }

    private Transaction buildTransaction(TransactionReq req, User user, Wallet wallet, Category category) {
        Transaction tx = new Transaction();
        tx.setUser(user);
        applyNewTransactionData(tx, req, wallet, category);
        return tx;
    }

    private void applyNewTransactionData(Transaction tx, TransactionReq req, Wallet wallet, Category category) {
        tx.setWallet(wallet);
        tx.setCategory(category);
        tx.setType(req.getType());
        tx.setAmount(req.getAmount());
        tx.setTransactionDate(req.getTransactionDate());
        tx.setNote(req.getNote());
    }

    private void applyBalanceChange(Wallet wallet, TransactionType type, BigDecimal amount) {
        BigDecimal newBalance = (type == TransactionType.INCOME)
                ? wallet.getMoney().add(amount)
                : wallet.getMoney().subtract(amount);
        wallet.setMoney(newBalance);
        walletRepository.save(wallet);
    }

    private void reverseBalanceChange(Wallet wallet, TransactionType type, BigDecimal amount) {
        BigDecimal newBalance = (type == TransactionType.INCOME)
                ? wallet.getMoney().subtract(amount)
                : wallet.getMoney().add(amount);
        wallet.setMoney(newBalance);
        walletRepository.save(wallet);
    }

    private void rollbackOldBalance(Transaction tx) {
        reverseBalanceChange(tx.getWallet(), tx.getType(), tx.getAmount());
    }

    public ResTransaction convertToResTransaction(Transaction tx) {
        return new ResTransaction(
                tx.getId(),
                tx.getWallet().getId(),
                tx.getWallet().getName(),
                tx.getCategory().getId(),
                tx.getCategory().getName(),
                tx.getType(),
                tx.getAmount(),
                tx.getTransactionDate(),
                tx.getNote()
        );
    }

    @Transactional
    public ResTransaction newTransaction(TransactionReq req) {
        User user = getCurrentUser();
        Wallet wallet = getOwnedWallet(req.getWalletId(), user.getId());
        Category category = getOwnedCategory(req.getCategoryId(), user.getId());
        validateCategoryMatchesType(category, req.getType());

        Transaction saved = transactionRepository.save(buildTransaction(req, user, wallet, category));
        applyBalanceChange(wallet, req.getType(), req.getAmount());

        return convertToResTransaction(saved);
    }

    public ResultPaginationDTO getAllTransactionByUser(Pageable pageable) {
        
        User user = getCurrentUser();
        Page<Transaction> pageTransaction = this.transactionRepository.getAllTransactionByUserId(user.getId(), pageable) ;

        ResultPaginationDTO res = new ResultPaginationDTO() ;

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setCurrentPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotalElements(pageTransaction.getTotalElements());
        meta.setTotalPages(pageTransaction.getTotalPages());

        List<ResTransaction> listTransaction = pageTransaction.getContent().stream().map(this::convertToResTransaction).toList();

        res.setMeta(meta);
        res.setResult(listTransaction);

        return res ;
    }

    public ResTransaction getDetailTransaction(Long id) {
        User user = getCurrentUser();
        Transaction tx = getOwnedTransaction(id, user.getId());
        return convertToResTransaction(tx);
    }

    @Transactional
    public ResTransaction updateTransaction(Long id, TransactionReq req) {
        User user = getCurrentUser();

        Transaction tx = getOwnedTransaction(id, user.getId());
        Wallet newWallet = getOwnedWallet(req.getWalletId(), user.getId());
        Category newCategory = getOwnedCategory(req.getCategoryId(), user.getId());
        validateCategoryMatchesType(newCategory, req.getType());

        rollbackOldBalance(tx);
        applyNewTransactionData(tx, req, newWallet, newCategory);
        Transaction updated = transactionRepository.save(tx);
        applyBalanceChange(newWallet, req.getType(), req.getAmount());

        return convertToResTransaction(updated);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        User user = getCurrentUser();
        Transaction tx = getOwnedTransaction(id, user.getId());

        rollbackOldBalance(tx);
        transactionRepository.delete(tx);
    }

    // Upload / Xem / Xoá hoá đơn
    public ResUploadFileDTO uploadReceipt(Long transactionId, MultipartFile file)
            throws URISyntaxException, IOException, StorageException {
        User user = getCurrentUser();
        Transaction tx = getOwnedTransaction(transactionId, user.getId());

        validateReceiptFile(file);
        deleteOldReceiptIfExists(tx);

        fileService.createDirectory(baseURI + RECEIPT_FOLDER);
        String fileName = fileService.store(file, RECEIPT_FOLDER);

        tx.setReceiptUrl(fileName);
        transactionRepository.save(tx);

        return new ResUploadFileDTO(fileName, Instant.now());
    }

    public ResponseEntity<Resource> getReceipt(Long transactionId)
        throws URISyntaxException, FileNotFoundException, StorageException {
        User user = getCurrentUser();
        Transaction tx = getOwnedTransaction(transactionId, user.getId());

        if (tx.getReceiptUrl() == null) {
            throw new StorageException("Giao dịch này chưa có hoá đơn đính kèm.");
        }

        String fileName = tx.getReceiptUrl();
        long fileLength = fileService.getFileLength(fileName, RECEIPT_FOLDER);
        if (fileLength == 0) {
            throw new StorageException("File with name = " + fileName + " not found.");
        }

        InputStreamResource resource = fileService.getResource(fileName, RECEIPT_FOLDER);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentLength(fileLength)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    public void deleteReceipt(Long transactionId) throws URISyntaxException {
        User user = getCurrentUser();
        Transaction tx = getOwnedTransaction(transactionId, user.getId());

        if (tx.getReceiptUrl() == null) {
            throw new ResourceNotFoundException("Giao dịch này chưa có hoá đơn đính kèm");
        }

        fileService.delete(tx.getReceiptUrl(), RECEIPT_FOLDER);
        tx.setReceiptUrl(null);
        transactionRepository.save(tx);
    }

    // thay thế old receipt
    private void deleteOldReceiptIfExists(Transaction tx) throws URISyntaxException {
        if (tx.getReceiptUrl() != null) {
            fileService.delete(tx.getReceiptUrl(), RECEIPT_FOLDER);
        }
    }

    // ktr file có valid k
    private void validateReceiptFile(MultipartFile file) throws StorageException {
        if (file == null || file.isEmpty()) {
            throw new StorageException("File không được để trống");
        }
        if (file.getSize() > MAX_RECEIPT_SIZE) {
            throw new StorageException("File không được vượt quá 5MB");
        }

        String fileName = file.getOriginalFilename();
        boolean isValid = fileName != null &&
                ALLOWED_EXTENSIONS.stream().anyMatch(ext -> fileName.toLowerCase().endsWith("." + ext));

        if (!isValid) {
            throw new StorageException("Chỉ chấp nhận file: " + ALLOWED_EXTENSIONS);
        }
    }

    // tạo csv
    public ResponseEntity<Resource> exportTransactionsToCsv(LocalDate fromDate, LocalDate toDate) {
        User user = getCurrentUser();

        List<Transaction> transactions = transactionRepository
                .findByUser_IdAndTransactionDateBetween(user.getId(), fromDate, toDate);

        StringBuilder csv = new StringBuilder();
        csv.append("Ngày,Loại,Danh mục,Ví,Số tiền,Ghi chú\n");

        for (Transaction tx : transactions) {
            csv.append(tx.getTransactionDate()).append(",")
            .append(tx.getType()).append(",")
            .append(escapeCsv(tx.getCategory().getName())).append(",")
            .append(escapeCsv(tx.getWallet().getName())).append(",")
            .append(tx.getAmount()).append(",")
            .append(escapeCsv(tx.getNote() != null ? tx.getNote() : ""))
            .append("\n");
        }

        // Thêm BOM (Byte Order Mark) để Excel hiển thị đúng tiếng Việt có dấu
        byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] finalBytes = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, finalBytes, 0, bom.length);
        System.arraycopy(csvBytes, 0, finalBytes, bom.length, csvBytes.length);

        ByteArrayResource resource = new ByteArrayResource(finalBytes);
        String fileName = "transactions_" + fromDate + "_" + toDate + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentLength(finalBytes.length)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    // Xử lý field có chứa dấu phẩy/xuống dòng, tránh phá cấu trúc CSV
    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}