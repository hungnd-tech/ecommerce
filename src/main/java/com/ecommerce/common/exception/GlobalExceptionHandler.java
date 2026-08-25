package com.ecommerce.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

// Spring tự chọn handler khớp CỤ THỂ NHẤT với loại exception, không quan tâm thứ tự khai báo trong file.
@RestControllerAdvice
// ưu tiên CAO HƠN Handler mặc định của Spring
// để advice của mình được chọn trước khi 2 bên cùng khớp 1 loại exception
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

//    public GlobalExceptionHandler() {
//         System.out.println(">>> GlobalExceptionHandler BEAN DA DUOC TAO");
//    }

    // ResponseStatusException tự nó đã có sẵn ProblemDetail đúng status/message (Spring build sẵn) -
    // chỉ lấy lại, không tự bịa. Khai rõ handler này để nó nằm cùng 1 chỗ với các handler khác bên dưới,
    // và để gắn thêm "instance" (đường dẫn request) cho đầy đủ field RFC 7807.
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        // System.out.println(">>> handleResponseStatus DUOC GOI");
        ProblemDetail problem = ex.getBody();
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    // @PreAuthorize sai role -> AccessDeniedException. Mặc định KHÔNG đi qua ProblemDetail,
    // rơi ra format lỗi whitelabel khác hẳn các lỗi còn lại.
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        // System.out.println(">>> handleAccessDenied DUOC GOI");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện hành động này");
        problem.setTitle("Forbidden");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    // @Valid sai (vd @NotBlank, @Min...) -> liệt kê rõ field nào sai, thay vì message chung chung
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        // System.out.println(">>> handleValidation DUOC GOI");
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ");
        problem.setTitle("Validation Failed");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    // Client gửi JSON sai định dạng/sai kiểu (vd gửi object thay vì số cho 1 field Long) -> lỗi CLIENT, phải là 400
    // không phải 500 (500 ngụ ý lỗi phía server, gây hiểu nhầm hệ thống đang có bug khi thực ra là do request sai)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu JSON gửi lên không đúng định dạng");
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    // Lưới an toàn cuối cùng: bug thật không lường trước (NPE, lỗi logic...).
    // Log đầy đủ để tự debug, nhưng KHÔNG trả chi tiết đó ra client (tránh rò rỉ thông tin nội bộ).
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        // System.out.println(">>> handleUnexpected DUOC GOI, exception thuc su la: " + ex.getClass().getName());
        log.error("Lỗi không lường trước tại {}", request.getRequestURI(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Đã có lỗi xảy ra, vui lòng thử lại sau");
        problem.setTitle("Internal Server Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
