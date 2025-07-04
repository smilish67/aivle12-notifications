package mp.infra;

import lombok.Data;
import mp.domain.Notification;
import mp.domain.NotificationRepository;
import mp.util.UserHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.*;

@RestController
@RequestMapping("/notifications")
// @CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true") // Gateway에서 CORS 처리
@Transactional
public class NotificationController {

    @Autowired
    NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<?> getNotifications(HttpServletRequest request) {
        System.out.println("=== NotificationController.getNotifications() START ===");
        
        try {
            // 인증 확인
            System.out.println("Checking authentication...");
            if (!UserHeaderUtil.isAuthenticated(request)) {
                System.out.println("Authentication failed - returning 401");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다."));
            }
            
            System.out.println("Authentication successful - getting user ID...");
            UUID userUUID = UserHeaderUtil.getUserId(request);
            System.out.println("User ID: " + userUUID);
            
            if (userUUID == null) {
                System.out.println("User ID is null - returning 401");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "사용자 정보를 가져올 수 없습니다."));
            }
            
            System.out.println("Fetching notifications for user: " + userUUID);
            List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userUUID);
            System.out.println("Found " + list.size() + " notifications");

            List<NotificationResponse> result = new ArrayList<>();
            for (Notification notification : list) {
                System.out.println("Processing notification ID: " + notification.getId());
                NotificationResponse res = new NotificationResponse();
                res.setNotificationId(notification.getId());
                res.setMessage(notification.getMessage());
                res.setIsRead(notification.getIsRead());
                res.setCreatedAt(notification.getCreatedAt());
                result.add(res);
            }

            System.out.println("Returning " + result.size() + " notification responses");
            System.out.println("=== NotificationController.getNotifications() SUCCESS ===");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.err.println("=== NotificationController.getNotifications() ERROR ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "알림 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(HttpServletRequest request) {
        System.out.println("=== NotificationController.getUnreadCount() START ===");
        
        try {
            // 인증 확인
            System.out.println("Checking authentication...");
            if (!UserHeaderUtil.isAuthenticated(request)) {
                System.out.println("Authentication failed - returning 401");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다."));
            }
            
            System.out.println("Authentication successful - getting user ID...");
            UUID userUUID = UserHeaderUtil.getUserId(request);
            System.out.println("User ID: " + userUUID);
            
            if (userUUID == null) {
                System.out.println("User ID is null - returning 401");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "사용자 정보를 가져올 수 없습니다."));
            }
            
            System.out.println("Fetching unread notifications for user: " + userUUID);
            List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userUUID);
            System.out.println("Found " + unreadNotifications.size() + " unread notifications");
            
            UnreadCountResponse response = new UnreadCountResponse();
            response.setCount(unreadNotifications.size());
            
            System.out.println("=== NotificationController.getUnreadCount() SUCCESS ===");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("=== NotificationController.getUnreadCount() ERROR ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "읽지 않은 알림 수 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PatchMapping("/read")
    public ResponseEntity<?> markAsRead(@RequestBody NotificationIdRequest request, HttpServletRequest httpRequest) {
        System.out.println("=== NotificationController.markAsRead() START ===");
        System.out.println("Request notification ID: " + request.getNotificationId());
        
        try {
            // 인증 확인
            System.out.println("Checking authentication...");
            if (!UserHeaderUtil.isAuthenticated(httpRequest)) {
                System.out.println("Authentication failed - returning 401");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다."));
            }
            
            System.out.println("Authentication successful - getting user ID...");
            UUID currentUserId = UserHeaderUtil.getUserId(httpRequest);
            System.out.println("Current user ID: " + currentUserId);
            
            if (currentUserId == null) {
                System.out.println("User ID is null - returning 401");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "사용자 정보를 가져올 수 없습니다."));
            }
            
            System.out.println("Looking up notification with ID: " + request.getNotificationId());
            Optional<Notification> optionalNotification = notificationRepository.findById(request.getNotificationId());
            
            if (optionalNotification.isPresent()) {
                Notification notification = optionalNotification.get();
                System.out.println("Found notification - owner: " + notification.getUserId() + ", current user: " + currentUserId);
                
                // 현재 사용자의 알림인지 확인
                if (notification.getUserId().equals(currentUserId)) {
                    System.out.println("User owns this notification - marking as read");
                    notification.setIsRead(true);
                    notificationRepository.save(notification);
                    System.out.println("=== NotificationController.markAsRead() SUCCESS ===");
                    return ResponseEntity.ok().build();
                } else {
                    System.out.println("User does not own this notification - returning 403");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "다른 사용자의 알림을 수정할 수 없습니다."));
                }
            } else {
                System.out.println("Notification not found - returning 404");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "알림을 찾을 수 없습니다."));
            }
            
        } catch (Exception e) {
            System.err.println("=== NotificationController.markAsRead() ERROR ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "알림 읽음 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(HttpServletRequest request) {
        System.out.println("=== NotificationController.markAllAsRead() START ===");
        
        try {
            // 인증 확인
            System.out.println("Checking authentication...");
            if (!UserHeaderUtil.isAuthenticated(request)) {
                System.out.println("Authentication failed - returning 401");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다."));
            }
            
            System.out.println("Authentication successful - getting user ID...");
            UUID userUUID = UserHeaderUtil.getUserId(request);
            System.out.println("User ID: " + userUUID);
            
            if (userUUID == null) {
                System.out.println("User ID is null - returning 401");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "사용자 정보를 가져올 수 없습니다."));
            }
            
            System.out.println("Fetching all notifications for user: " + userUUID);
            List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userUUID);
            System.out.println("Found " + list.size() + " notifications to mark as read");
            
            for (Notification notification : list) {
                System.out.println("Marking notification " + notification.getId() + " as read");
                notification.setIsRead(true);
            }
            
            System.out.println("Saving all notifications...");
            notificationRepository.saveAll(list);
            
            System.out.println("=== NotificationController.markAllAsRead() SUCCESS ===");
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            System.err.println("=== NotificationController.markAllAsRead() ERROR ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "모든 알림 읽음 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @Data
    public static class NotificationIdRequest {
        private UUID notificationId;
    }

    @Data
    public static class NotificationResponse {
        private UUID notificationId;
        private String message;
        private Boolean isRead;
        private Date createdAt;
    }

    @Data
    public static class UnreadCountResponse {
        private int count;
    }
}
