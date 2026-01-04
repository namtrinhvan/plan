package tds.op.plan.structure;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity

public class Unit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private String head; //Email của người đứng đầu, reference sang bảng staff chứ không phải user.
    private UnitType level;
    private long parentId;
    //
}
