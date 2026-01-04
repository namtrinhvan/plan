package tds.op.plan.structure.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tds.op.plan.structure.UnitDTO;

import java.util.List;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
// @CrossOrigin(origins = "*") // Bật dòng này nếu bạn dev UI ở port khác (VD: localhost:3000)
public class UnitController {

    private final UnitService unitService;

    // ==========================================
    // 1. BULK OPERATIONS (Thao tác cả cây)
    // ==========================================

    /**
     * Lấy toàn bộ cấu trúc cây tổ chức (Full Hierarchy)
     * GET /api/units/structure
     */
    @GetMapping("/structure")
    public ResponseEntity<List<UnitDTO>> getStructure() {
        List<UnitDTO> structure = unitService.getFullStructure();
        return ResponseEntity.ok(structure);
    }

    /**
     * Tạo hoặc Cập nhật cả một nhánh/cây tổ chức (Bulk Save)
     * POST /api/units/structure
     */
    @PostMapping("/structure")
    public ResponseEntity<UnitDTO> saveStructure(@RequestBody UnitDTO unitDTO) {
        UnitDTO savedUnit = unitService.saveStructure(unitDTO);
        return ResponseEntity.ok(savedUnit);
    }

    // ==========================================
    // 2. GRANULAR OPERATIONS (Thao tác lẻ)
    // ==========================================

    /**
     * Tạo mới một Unit đơn lẻ
     * POST /api/units
     */
    @PostMapping
    public ResponseEntity<UnitDTO> createUnit(@RequestBody UnitDTO unitDTO) {
        UnitDTO createdUnit = unitService.createUnit(unitDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUnit);
    }

    /**
     * Cập nhật thông tin một Unit
     * PUT /api/units/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UnitDTO> updateUnit(@PathVariable Long id, @RequestBody UnitDTO unitDTO) {
        UnitDTO updatedUnit = unitService.updateUnit(id, unitDTO);
        return ResponseEntity.ok(updatedUnit);
    }

    /**
     * Xóa một Unit (Chỉ xóa được nếu không có con)
     * DELETE /api/units/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUnit(@PathVariable Long id) {
        unitService.deleteUnit(id);
        return ResponseEntity.noContent().build(); // Trả về 204 No Content
    }

    // ==========================================
    // 3. STAFF MANAGEMENT (Quản lý nhân sự trong Unit)
    // ==========================================

    /**
     * Thêm nhân viên vào Unit
     * POST /api/units/{unitId}/staffs/{staffId}
     */
    @PostMapping("/{unitId}/staffs/{staffId}")
    public ResponseEntity<String> addStaffToUnit(@PathVariable Long unitId, @PathVariable Long staffId) {
        unitService.addStaffToUnit(unitId, staffId);
        return ResponseEntity.ok("Staff added successfully");
    }

    /**
     * Xóa nhân viên khỏi Unit
     * DELETE /api/units/{unitId}/staffs/{staffId}
     */
    @DeleteMapping("/{unitId}/staffs/{staffId}")
    public ResponseEntity<Void> removeStaffFromUnit(@PathVariable Long unitId, @PathVariable Long staffId) {
        unitService.removeStaffFromUnit(unitId, staffId);
        return ResponseEntity.noContent().build();
    }
}