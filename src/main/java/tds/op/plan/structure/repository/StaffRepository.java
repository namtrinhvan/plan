package tds.op.plan.structure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tds.op.plan.structure.Staff;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
}
