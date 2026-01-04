package tds.op.plan.structure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tds.op.plan.structure.Unit;
import java.util.List;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {
    // Kiểm tra xem Unit có con không (để chặn xóa nếu cần)
    boolean existsByParentId(Long parentId);
    
    // Lấy danh sách con trực tiếp
    List<Unit> findByParentId(Long parentId);
}