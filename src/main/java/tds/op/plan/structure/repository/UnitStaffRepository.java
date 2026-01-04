package tds.op.plan.structure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tds.op.plan.structure.UnitStaff;
import java.util.Optional;

@Repository
public interface UnitStaffRepository extends JpaRepository<UnitStaff, Long> {
    // ... các hàm cũ
    
    // Tìm mapping cụ thể để xóa
    Optional<UnitStaff> findByUnitIdAndStaffId(Long unitId, Long staffId);
    
    // Xóa tất cả nhân sự thuộc unit (dùng khi xóa unit)
    void deleteByUnitId(Long unitId);
    
    // Kiểm tra nhân sự đã tồn tại trong unit chưa
    boolean existsByUnitIdAndStaffId(Long unitId, Long staffId);
}