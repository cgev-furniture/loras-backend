package am.loras.backend.repository;

import am.loras.backend.domain.ContactInquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactInquiryRepository extends JpaRepository<ContactInquiry, Long> {

    Page<ContactInquiry> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    long countByReadFalse();
}
