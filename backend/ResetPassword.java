import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class ResetPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String plainPassword = "password";
        String encoded = encoder.encode(plainPassword);
        System.out.println("Encoded password for 'password':");
        System.out.println(encoded);
        System.out.println("Length: " + encoded.length());
    }
}
