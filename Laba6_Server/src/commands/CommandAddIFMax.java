package commands;

import data.*;
import collection.*;

public class CommandAddIFMax extends AbstractCommand {

    public CommandAddIFMax(MyCollection collection) {
        super(collection);
    }

    @Override
    public String execute() {
        Vehicle vehicleAddIfMax = (Vehicle) super.getParameter();
        return super.getCollection().addIfMax(vehicleAddIfMax);
    }
}
