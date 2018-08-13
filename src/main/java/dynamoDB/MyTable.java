package dynamoDB;

import java.util.ArrayList;
import java.util.List;

public class MyTable {
    public List<Attribute> attributes = new ArrayList<>();

    public MyTable() {
        attributes.add(new Attribute("fileType", "S"));
        attributes.add(new Attribute("filePath", "S"));
        attributes.add(new Attribute("originTimeStamp", "N"));
        attributes.add(new Attribute("packageId", "S"));
    }
}
