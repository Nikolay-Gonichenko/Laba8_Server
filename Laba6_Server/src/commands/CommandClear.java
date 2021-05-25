package commands;
import collection.*;

import java.util.List;


public class CommandClear extends AbstractCommand {

    public CommandClear(MyCollection myCollection) {
        super(myCollection);
    }

    @Override
    public String execute() {
        return super.getCollection().clear((List<Integer>) getParameter());
    }
}
