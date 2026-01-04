package tds.op.plan.structure;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UnitDTO {
    private long id;
    private String name;
    private String head; //Email của người đứng đầu
    private UnitType level;
    private UnitDTO parent; //Object này không bao gồm children để tránh circular reference.
    private List<UnitDTO> children = new ArrayList<>();
}
