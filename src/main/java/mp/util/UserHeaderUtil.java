package mp.util;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * Gateway에서 전달한 사용자 정보 헤더를 읽는 유틸리티 클래스
 */
public class UserHeaderUtil {
    
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String USER_SUBSCRIBED_HEADER = "X-User-Subscribed";
    
    /**
     * Gateway에서 전달한 사용자 ID를 추출
     */
    public static UUID getUserId(HttpServletRequest request) {
        String userIdStr = request.getHeader(USER_ID_HEADER);
        System.out.println("=== UserHeaderUtil.getUserId() ===");
        System.out.println("X-User-Id header: " + userIdStr);
        
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                UUID userId = UUID.fromString(userIdStr);
                System.out.println("Successfully parsed User ID: " + userId);
                return userId;
            } catch (IllegalArgumentException e) {
                System.err.println("Failed to parse User ID: " + userIdStr);
                throw new RuntimeException("유효하지 않은 사용자 ID: " + userIdStr, e);
            }
        }
        System.out.println("No User ID found in headers");
        return null;
    }
    
    /**
     * Gateway에서 전달한 사용자 역할을 추출
     */
    public static String getUserRole(HttpServletRequest request) {
        String role = request.getHeader(USER_ROLE_HEADER);
        System.out.println("=== UserHeaderUtil.getUserRole() ===");
        System.out.println("X-User-Role header: " + role);
        return role;
    }
    
    /**
     * Gateway에서 전달한 구독 상태를 추출
     */
    public static Boolean isUserSubscribed(HttpServletRequest request) {
        String subscribedStr = request.getHeader(USER_SUBSCRIBED_HEADER);
        System.out.println("=== UserHeaderUtil.isUserSubscribed() ===");
        System.out.println("X-User-Subscribed header: " + subscribedStr);
        
        if (subscribedStr != null && !subscribedStr.isEmpty()) {
            Boolean result = Boolean.parseBoolean(subscribedStr);
            System.out.println("Parsed subscription status: " + result);
            return result;
        }
        System.out.println("No subscription status found, returning false");
        return false;
    }
    
    /**
     * 사용자 인증 여부 확인 (헤더에 사용자 ID가 있는지 확인)
     */
    public static boolean isAuthenticated(HttpServletRequest request) {
        boolean authenticated = getUserId(request) != null;
        System.out.println("=== UserHeaderUtil.isAuthenticated() ===");
        System.out.println("User authenticated: " + authenticated);
        return authenticated;
    }
    
    /**
     * 관리자 권한 확인
     */
    public static boolean isAdmin(HttpServletRequest request) {
        String role = getUserRole(request);
        boolean isAdmin = "ADMIN".equals(role);
        System.out.println("=== UserHeaderUtil.isAdmin() ===");
        System.out.println("Is admin: " + isAdmin);
        return isAdmin;
    }
    
    /**
     * 작가 권한 확인
     */
    public static boolean isAuthor(HttpServletRequest request) {
        String role = getUserRole(request);
        boolean isAuthor = "AUTHOR".equals(role);
        System.out.println("=== UserHeaderUtil.isAuthor() ===");
        System.out.println("Is author: " + isAuthor);
        return isAuthor;
    }
    
    /**
     * 사용자 권한 확인
     */
    public static boolean isUser(HttpServletRequest request) {
        String role = getUserRole(request);
        boolean isUser = "USER".equals(role);
        System.out.println("=== UserHeaderUtil.isUser() ===");
        System.out.println("Is user: " + isUser);
        return isUser;
    }
    
    /**
     * 특정 사용자 권한 확인 (자신의 정보인지 또는 관리자인지)
     */
    public static boolean hasAccessToUser(HttpServletRequest request, UUID targetUserId) {
        UUID currentUserId = getUserId(request);
        boolean hasAccess = currentUserId != null && 
               (currentUserId.equals(targetUserId) || isAdmin(request));
        System.out.println("=== UserHeaderUtil.hasAccessToUser() ===");
        System.out.println("Has access to user " + targetUserId + ": " + hasAccess);
        return hasAccess;
    }
    
    /**
     * 권한 체크 (USER, AUTHOR, ADMIN 중 하나라도 해당하는지)
     */
    public static boolean hasAnyRole(HttpServletRequest request, String... roles) {
        String userRole = getUserRole(request);
        if (userRole == null) return false;
        
        for (String role : roles) {
            if (role.equals(userRole)) {
                System.out.println("=== UserHeaderUtil.hasAnyRole() ===");
                System.out.println("User has role: " + role);
                return true;
            }
        }
        System.out.println("=== UserHeaderUtil.hasAnyRole() ===");
        System.out.println("User does not have any of the required roles");
        return false;
    }
} 