package dynamoDB;

import java.util.ArrayList;
import java.util.List;

import static enums.TableDescription.*;

public class MyTable {
    public List<Attribute> attributes = new ArrayList<>();

    public MyTable() {
        attributes.add(new Attribute(SECONDATY_INDEX_2.key, SECONDATY_INDEX_2.type));
        attributes.add(new Attribute(SECONDATY_INDEX_1.key, SECONDATY_INDEX_1.type));
        attributes.add(new Attribute(SORT.key, SORT.type));
        attributes.add(new Attribute(PARTITION.key, PARTITION.type));
    }
}
