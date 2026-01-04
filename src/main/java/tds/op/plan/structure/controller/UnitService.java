package tds.op.plan.structure.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tds.op.plan.structure.*;
import tds.op.plan.structure.repository.StaffRepository;
import tds.op.plan.structure.repository.UnitRepository;
import tds.op.plan.structure.repository.UnitStaffRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitRepository unitRepository;
    private final UnitStaffRepository unitStaffRepository;
    private final StaffRepository staffRepository; // Inject trực tiếp hoặc qua StaffService
    private final StaffService staffService;

    // =========================================================================
    // PHẦN 1: BULK OPERATIONS (Tạo/Load cả cây tổ chức)
    // =========================================================================

    /**
     * Load toàn bộ cấu trúc cây tổ chức.
     * Tối ưu hóa: Chỉ thực hiện 3 query xuống DB, ghép cây trên RAM.
     */
    public List<UnitDTO> getFullStructure() {
        // 1. Fetch toàn bộ dữ liệu thô
        List<Unit> allUnits = unitRepository.findAll();
        List<UnitStaff> allUnitStaffs = unitStaffRepository.findAll();
        Map<Long, StaffDTO> staffMap = staffService.getStaffMap(); // Lấy map nhân sự

        // 2. Map nhân sự vào từng Unit
        // Tạo Map: UnitID -> List<StaffDTO>
        Map<Long, List<StaffDTO>> unitStaffMap = new HashMap<>();
        for (UnitStaff us : allUnitStaffs) {
            StaffDTO staff = staffMap.get(us.getStaffId());
            if (staff != null) {
                unitStaffMap.computeIfAbsent(us.getUnitId(), k -> new ArrayList<>()).add(staff);
            }
        }

        // 3. Convert Unit Entity -> DTO và gán Staff
        Map<Long, UnitDTO> dtoMap = new HashMap<>();
        List<UnitDTO> allDtos = new ArrayList<>();

        for (Unit unit : allUnits) {
            UnitDTO dto = mapToDTO(unit);
            // Gán danh sách nhân viên chi tiết
            List<StaffDTO> staffs = unitStaffMap.getOrDefault(unit.getId(), new ArrayList<>());
            dto.setStaffs(staffs);
            // Gán danh sách ID để FE dễ xử lý form edit
            dto.setStaffIds(staffs.stream().map(StaffDTO::getId).collect(Collectors.toList()));

            dtoMap.put(unit.getId(), dto);
            allDtos.add(dto);
        }

        // 4. Xây dựng cấu trúc cây (Parent - Children)
        List<UnitDTO> roots = new ArrayList<>();
        for (UnitDTO dto : allDtos) {
            // Tìm parentId từ entity gốc (lấy lại từ list ban đầu hoặc query map)
            Long parentId = findParentId(dto.getId(), allUnits);
            
            if (parentId == null || parentId == 0) {
                roots.add(dto); // Là Node gốc (thường là Department)
            } else {
                UnitDTO parentDto = dtoMap.get(parentId);
                if (parentDto != null) {
                    parentDto.getChildren().add(dto);
                    dto.setParent(parentDto); // Set parent reference (nhớ dùng @JsonIgnore ở DTO)
                } else {
                    // Trường hợp data lỗi (mồ côi cha), đưa lên làm root tạm để không mất dữ liệu
                    roots.add(dto);
                }
            }
        }
        return roots;
    }

    /**
     * Lưu cả cấu trúc cây (Tạo mới hoặc Update đệ quy).
     * Input: Root Node của cây.
     */
    @Transactional
    public UnitDTO saveStructure(UnitDTO rootDto) {
        return saveUnitRecursive(rootDto, 0L);
    }

    private UnitDTO saveUnitRecursive(UnitDTO dto, long parentId) {
        // 1. Lưu Unit
        Unit unit = new Unit();
        if (dto.getId() != null && dto.getId() > 0) {
            unit = unitRepository.findById(dto.getId()).orElse(new Unit());
        }
        unit.setName(dto.getName());
        unit.setHead(dto.getHead());
        unit.setLevel(dto.getLevel());
        unit.setParentId(parentId);
        
        unit = unitRepository.save(unit); // Có ID sau khi save

        // 2. Lưu quan hệ nhân sự (Xóa cũ, thêm mới để đảm bảo đồng bộ tuyệt đối)
        updateUnitStaffRelations(unit.getId(), dto.getStaffIds());

        // 3. Đệ quy lưu con
        List<UnitDTO> savedChildren = new ArrayList<>();
        if (dto.getChildren() != null) {
            for (UnitDTO childDto : dto.getChildren()) {
                validateHierarchy(unit.getLevel(), childDto.getLevel());
                savedChildren.add(saveUnitRecursive(childDto, unit.getId()));
            }
        }

        // 4. Trả về kết quả
        UnitDTO result = mapToDTO(unit);
        result.setChildren(savedChildren);
        result.setStaffIds(dto.getStaffIds());
        return result;
    }

    // =========================================================================
    // PHẦN 2: GRANULAR OPERATIONS (Thao tác đơn lẻ linh hoạt)
    // =========================================================================

    @Transactional
    public UnitDTO createUnit(UnitDTO dto) {
        Unit unit = new Unit();
        unit.setName(dto.getName());
        unit.setHead(dto.getHead());
        unit.setLevel(dto.getLevel());

        // Validate Parent
        if (dto.getParent() != null && dto.getParent().getId() != null) {
            Unit parent = unitRepository.findById(dto.getParent().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent Unit not found"));
            validateHierarchy(parent.getLevel(), unit.getLevel());
            unit.setParentId(parent.getId());
        } else {
            unit.setParentId(0L);
        }

        unit = unitRepository.save(unit);

        // Lưu staff nếu có
        if (dto.getStaffIds() != null && !dto.getStaffIds().isEmpty()) {
            addStaffListToUnit(unit.getId(), dto.getStaffIds());
        }

        return mapToDTO(unit);
    }

    @Transactional
    public UnitDTO updateUnit(Long id, UnitDTO dto) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Unit not found with id: " + id));

        unit.setName(dto.getName());
        unit.setHead(dto.getHead());
        // Level thường ít khi đổi, nhưng nếu đổi cần validate lại hierarchy

        // Logic di chuyển (Move Unit)
        if (dto.getParent() != null && dto.getParent().getId() != null) {
            Long newParentId = dto.getParent().getId();
            if (newParentId != unit.getParentId()) {
                if (newParentId.equals(unit.getId())) {
                    throw new IllegalArgumentException("Cannot set unit as its own parent");
                }
                // Check validate cấp bậc với cha mới
                Unit newParent = unitRepository.findById(newParentId)
                        .orElseThrow(() -> new EntityNotFoundException("New Parent Unit not found"));
                validateHierarchy(newParent.getLevel(), unit.getLevel());
                
                unit.setParentId(newParentId);
            }
        }

        unit = unitRepository.save(unit);
        return mapToDTO(unit);
    }

    @Transactional
    public void deleteUnit(Long id) {
        if (!unitRepository.existsById(id)) {
            throw new EntityNotFoundException("Unit not found");
        }
        // Chặn xóa nếu còn con (Safe Delete)
        if (unitRepository.existsByParentId(id)) {
            throw new IllegalStateException("Cannot delete unit that contains children units.");
        }

        // Xóa mapping nhân sự trước
        unitStaffRepository.deleteByUnitId(id);
        // Xóa unit
        unitRepository.deleteById(id);
    }

    // =========================================================================
    // PHẦN 3: STAFF MANAGEMENT (Quản lý nhân sự trong Unit)
    // =========================================================================

    @Transactional
    public void addStaffToUnit(Long unitId, Long staffId) {
        verifyUnitAndStaffExist(unitId, staffId);
        
        if (!unitStaffRepository.existsByUnitIdAndStaffId(unitId, staffId)) {
            UnitStaff us = new UnitStaff();
            us.setUnitId(unitId);
            us.setStaffId(staffId);
            unitStaffRepository.save(us);
        }
    }

    @Transactional
    public void removeStaffFromUnit(Long unitId, Long staffId) {
        UnitStaff us = unitStaffRepository.findByUnitIdAndStaffId(unitId, staffId)
                .orElseThrow(() -> new EntityNotFoundException("Staff is not in this unit"));
        unitStaffRepository.delete(us);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void updateUnitStaffRelations(Long unitId, List<Long> staffIds) {
        // Xóa hết cũ
        unitStaffRepository.deleteByUnitId(unitId);
        
        // Thêm mới
        if (staffIds != null && !staffIds.isEmpty()) {
            addStaffListToUnit(unitId, staffIds);
        }
    }

    private void addStaffListToUnit(Long unitId, List<Long> staffIds) {
        List<UnitStaff> list = staffIds.stream().map(staffId -> {
            UnitStaff us = new UnitStaff();
            us.setUnitId(unitId);
            us.setStaffId(staffId);
            return us;
        }).collect(Collectors.toList());
        unitStaffRepository.saveAll(list);
    }

    private UnitDTO mapToDTO(Unit unit) {
        UnitDTO dto = new UnitDTO();
        dto.setId(unit.getId());
        dto.setName(unit.getName());
        dto.setHead(unit.getHead());
        dto.setLevel(unit.getLevel());
        return dto;
    }

    private Long findParentId(Long unitId, List<Unit> allUnits) {
        return allUnits.stream()
                .filter(u -> u.getId() == unitId)
                .findFirst()
                .map(Unit::getParentId)
                .orElse(0L);
    }

    private void verifyUnitAndStaffExist(Long unitId, Long staffId) {
        if (!unitRepository.existsById(unitId)) {
            throw new EntityNotFoundException("Unit not found");
        }
        if (!staffRepository.existsById(staffId)) {
            throw new EntityNotFoundException("Staff not found");
        }
    }

    private void validateHierarchy(UnitType parentType, UnitType childType) {
        // Rule: DEPARTMENT -> GROUP -> FUNCTION
        if (parentType == UnitType.DEPARTMENT && childType == UnitType.DEPARTMENT) {
             throw new IllegalArgumentException("Department cannot contain another Department");
        }
        if (parentType == UnitType.GROUP && (childType == UnitType.DEPARTMENT || childType == UnitType.GROUP)) {
             throw new IllegalArgumentException("Group can only contain Functions");
        }
        if (parentType == UnitType.FUNCTION) {
             throw new IllegalArgumentException("Function cannot contain sub-units");
        }
    }
}