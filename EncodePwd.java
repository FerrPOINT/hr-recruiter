import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class EncodePwd {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder(12).encode("adminpass"));
    }
} 