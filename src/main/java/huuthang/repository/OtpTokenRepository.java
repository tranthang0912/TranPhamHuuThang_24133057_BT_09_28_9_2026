package huuthang.repository;

import huuthang.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(
            String email, String type);

    void deleteByEmailAndType(String email, String type);

    Optional<OtpToken> findTopByEmailAndTypeOrderByCreatedAtDesc(String email, String type);
}
