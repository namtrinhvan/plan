package tds.op.plan.structure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tds.op.plan.structure.Unit;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

}
