package tds.op.plan.structure.controller; // Hoặc package service tùy project của bạn

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tds.op.plan.structure.Staff;
import tds.op.plan.structure.StaffDTO;
import tds.op.plan.structure.repository.StaffRepository; // Cần tạo repository này nếu chưa có

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;

    /**
     * Lấy danh sách tất cả nhân sự, chuyển đổi sang DTO.
     * Dùng để hiển thị dropdown chọn nhân sự trên UI.
     */
    public List<StaffDTO> getAllStaffs() {
        return staffRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Helper: Lấy Map (ID -> DTO) để UnitService sử dụng khi map dữ liệu bulk.
     * Giúp tra cứu O(1) thay vì query DB nhiều lần.
     */
    public Map<Long, StaffDTO> getStaffMap() {
        return staffRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toMap(StaffDTO::getId, staff -> staff));
    }

    /**
     * Chuyển đổi Entity sang DTO
     */
    public StaffDTO toDTO(Staff staff) {
        StaffDTO dto = new StaffDTO();
        dto.setId(staff.getId());
        dto.setName(staff.getName());
        dto.setEmail(staff.getEmail());
        dto.setPicture(staff.getPicture());
        return dto;
    }
}