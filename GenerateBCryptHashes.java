import java.security.SecureRandom;
import java.util.Base64;

/**
 * Simple BCrypt hash generator
 * This generates BCrypt hashes for the National IDs
 */
public class GenerateBCryptHashes {
    
    // BCrypt implementation
    static class BCrypt {
        private static final String BCRYPT_PATTERN = "\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}";
        private static final int ROUNDS = 10;
        
        public static String hashpw(String password, String salt) {
            return BCryptUtil.hashpw(password, salt);
        }
        
        public static String gensalt() {
            return gensalt(ROUNDS);
        }
        
        public static String gensalt(int rounds) {
            return BCryptUtil.gensalt(rounds);
        }
        
        public static boolean checkpw(String plaintext, String hashed) {
            return BCryptUtil.checkpw(plaintext, hashed);
        }
    }
    
    static class BCryptUtil {
        private static final String CHARSET = "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        
        public static String gensalt(int rounds) {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            
            StringBuilder sb = new StringBuilder();
            sb.append("$2a$");
            if (rounds < 10) sb.append("0");
            sb.append(rounds).append("$");
            
            for (int i = 0; i < 22; i++) {
                int index = (salt[i] & 0xFF) % CHARSET.length();
                sb.append(CHARSET.charAt(index));
            }
            
            return sb.toString();
        }
        
        public static String hashpw(String password, String salt) {
            // This is a simplified version - for production use Spring Security's BCryptPasswordEncoder
            // For now, we'll just return a placeholder that indicates we need the real implementation
            return null;
        }
        
        public static boolean checkpw(String plaintext, String hashed) {
            return false;
        }
    }
    
    public static void main(String[] args) {
        System.out.println("BCrypt Hash Generator");
        System.out.println("====================");
        System.out.println();
        System.out.println("National ID: 11111111");
        System.out.println("National ID: 87600321");
        System.out.println();
        System.out.println("Note: To generate actual BCrypt hashes, we need Spring Security's BCryptPasswordEncoder");
        System.out.println("which requires the Spring framework. Instead, use the following approach:");
        System.out.println();
        System.out.println("1. Run the backend application");
        System.out.println("2. Use the PasswordHashGenerator utility class in the backend");
        System.out.println("3. Or use an online BCrypt generator: https://bcrypt-generator.com/");
        System.out.println();
        System.out.println("For testing, here are example BCrypt hashes (strength 10):");
        System.out.println("National ID 11111111 -> $2a$10$...");
        System.out.println("National ID 87600321 -> $2a$10$...");
    }
}
