package tds.op.plan.structure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnitDTO {
    private Long id; // Chuyển sang Long object để handle null khi tạo mới
    private String name;
    private String head; // Email người đứng đầu
    private UnitType level;

    @JsonIgnore // Ẩn field này khi response dạng tree để tránh circular reference (vòng lặp vô hạn)
    private UnitDTO parent;

    private List<UnitDTO> children = new ArrayList<>();

    // Dùng để nhận input ID nhân sự khi tạo/sửa (Request)
    private List<Long> staffIds = new ArrayList<>();

    // Dùng để hiển thị chi tiết nhân sự khi trả về (Response)
    private List<StaffDTO> staffs = new ArrayList<>();
}