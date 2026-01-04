package tds.op.plan.structure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tds.op.plan.structure.UnitStaff;

import java.util.Collection;
import java.util.List;

@Repository
public interface UnitStaffRepository extends JpaRepository<UnitStaff, Long> {
    public List<UnitStaff> findByUnitId(Long id);
    public List<UnitStaff> findByUnitIdIn(Collection<Long> ids);
}
