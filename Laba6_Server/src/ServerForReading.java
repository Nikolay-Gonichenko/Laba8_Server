import Client.User;
import Connection.DataToOutput;
import Connection.MessageForClient;
import collection.MyCollection;
import commands.*;
import data.Vehicle;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Exchanger;

public class ServerForReading implements Runnable {
    private final SocketChannel clientDialog;
    private final DataBaseWorker worker;
    private MyCollection collection;
    private final Exchanger<MessageForClient> exchanger;
    private User user  = null;


    public ServerForReading(SocketChannel clientDialog, DataBaseWorker worker, MyCollection collection, Exchanger<MessageForClient> exchanger) {
        this.clientDialog = clientDialog;
        this.worker = worker;
        this.collection = collection;
        this.exchanger = exchanger;
    }

    @Override
    public void run() {
        while (true) {
            Switcher switcher = new Switcher();
            MessageForClient message = new MessageForClient();
            ByteBuffer buffer = ByteBuffer.allocate(65536);
            DataToOutput<?> command = read(buffer);
            try {
                String name = command.getCommandName();
                System.out.println(name);
                synchronized (collection) {
                    switch (name) {
                        case "help":
                            AbstractCommand<String> help = new CommandHelp(collection);
                            switcher.setCommand(help);
                            message.setCommandIsDone(true);
                            message.setMessage(switcher.doCommand());
                            break;
                        case "info":
                            AbstractCommand<String> info = new CommandInfo(collection);
                            switcher.setCommand(info);
                            message.setCommandIsDone(true);
                            message.setMessage(switcher.doCommand());
                            break;
                        case "show":
                            AbstractCommand<String> show = new CommandShow(collection);
                            switcher.setCommand(show);
                            message.setCommandIsDone(true);
                            message.setMessage(switcher.doCommand());
                            break;
                        case "add":
                            AbstractCommand<Vehicle> add = new CommandAdd(collection);
                            Vehicle v = (Vehicle) command.getArgument();
                            v.setUser(user.getUsername());
                            add.setParameter(v);
                            switcher.setCommand(add);
                            message.setCommandIsDone(true);
                            message.setMessage(switcher.doCommand());

                            worker.add(v);
                            System.out.println(collection.show());
                            break;
                        case "update":
                            int checkUpdate = worker.updateById((Vehicle) command.getArgument());
                            if (checkUpdate > 0) {
                                AbstractCommand<Vehicle> updateById = new CommandUpdateByID(collection);
                                updateById.setParameter((Vehicle) command.getArgument());
                                switcher.setCommand(updateById);
                                String s = switcher.doCommand();
                                message.setCommandIsDone(s.equals("Element is updated"));
                                message.setMessage(s);
                            } else {
                                message.setCommandIsDone(false);
                                message.setMessage("You don't have such element in your possession");
                            }


                            break;
                        case "remove_by_id":
                            boolean checkRemove = false;
                            if (!collection.checkToEmpty()) {
                                checkRemove = worker.removeById((Integer) command.getArgument());
                            }

                            if (checkRemove) {
                                AbstractCommand<Integer> removeById = new CommandRemoveById(collection);
                                removeById.setParameter((Integer) command.getArgument());
                                switcher.setCommand(removeById);
                                String removeId = switcher.doCommand();
                                message.setCommandIsDone(removeId.equals("Element was deleted"));
                                message.setMessage(removeId);
                            } else {
                                message.setCommandIsDone(false);
                                message.setMessage("You don't have such element in your possession");
                            }
                            break;
                        case "clear":
                            List<Integer> list=null;
                            if (!collection.checkToEmpty()) {
                                list = worker.clear();
                            }
                            AbstractCommand<List<Integer>> clear = new CommandClear(collection);
                            clear.setParameter(list);
                            switcher.setCommand(clear);
                            message.setCommandIsDone(!collection.checkToEmpty());
                            message.setMessage(switcher.doCommand());
                            break;
                        case "execute_script":
                            //AbstractCommand<File> execute_script = new CommandExecuteScript(collection, file);
                            //execute_script.setParameter((File) command.getArgument());
                            //switcher.setCommand(execute_script);
                            String sc = switcher.doCommand();
                            message.setCommandIsDone(!sc.equals("This file doesn't exist"));
                            message.setMessage(sc);
                            break;
                        case "remove_first":
                        case "remove_head":
                            int id = 0;
                            if (!collection.checkToEmpty()) {
                                id = worker.removeFirst();
                            }
                            if (id != 0) {
                                AbstractCommand<Integer> removeById2 = new CommandRemoveById(collection);
                                removeById2.setParameter(id);
                                switcher.setCommand(removeById2);
                                String removeId2 = switcher.doCommand();
                                message.setCommandIsDone(removeId2.equals("Element was deleted"));
                                message.setMessage(removeId2);
                            } else {
                                message.setCommandIsDone(false);
                                message.setMessage("You don't have your vehicles");
                            }
                            break;

                        case "add_if_max":
                            AbstractCommand<Vehicle> addIfMax = new CommandAddIFMax(collection);
                            addIfMax.setParameter((Vehicle) command.getArgument());
                            switcher.setCommand(addIfMax);
                            String addMax = switcher.doCommand();
                            message.setCommandIsDone(addMax.equals("Element was added"));
                            message.setMessage(addMax);

                            if (message.isCommandDone()) {
                                worker.add((Vehicle) command.getArgument());
                            }
                            break;
                        case "remove_any_by_fuel_type":
                            int idFuelType = worker.removeAnyByFuelType(command.getArgument().toString());
                            if (idFuelType != 0) {
                                AbstractCommand<Integer> removeById3 = new CommandRemoveById(collection);
                                removeById3.setParameter(idFuelType);
                                switcher.setCommand(removeById3);
                                String removeId3 = switcher.doCommand();
                                message.setCommandIsDone(removeId3.equals("Element was deleted"));
                                message.setMessage(removeId3);
                            } else {
                                message.setCommandIsDone(false);
                                message.setMessage("You don't have this vehicle in your possession");
                            }
                            break;
                        case "max_by_name":
                            AbstractCommand<String> maxByName = new CommandMaxByName(collection);
                            switcher.setCommand(maxByName);
                            message.setCommandIsDone(collection.checkToEmpty());
                            message.setMessage(switcher.doCommand());
                            break;
                        case "group_counting_by_creation_date":
                            AbstractCommand<String> groupCount = new CommandGroupCounting(collection);
                            switcher.setCommand(groupCount);
                            message.setCommandIsDone(!collection.checkToEmpty());
                            message.setMessage(switcher.doCommand());
                            break;
                        case "newUser":
                            user = (User) command.getArgument();
                            String answer = worker.addNewUser(user);
                            if (answer.equals("Something went wrong with authorization") || answer.equals("Client.User entered a wrong password")) {
                                message.setCommandIsDone(false);
                            } else {
                                message.setCommandIsDone(true);
                            }
                            message.setMessage(answer);
                            break;
                        case "getCollection":
                            message.setCommandIsDone(true);
                            message.setMessage(collectionToString());
                            break;
                        case "exit":
                            message.setCommandIsDone(true);
                            message.setMessage("Client went out");
                            break;
                    }
                    exchanger.exchange(message);
                }
            } catch (NullPointerException | InterruptedException e) {
                e.printStackTrace();
                System.out.println("Client went out");
                break;
            }
        }
    }
    private String collectionToString(){
        StringBuilder stringBuilder = new StringBuilder();
        for (Vehicle v:collection.getQueue()){
            System.out.println(v);
            System.out.println(v.getX());
            stringBuilder.append(v.getId()).append(",");
            stringBuilder.append(v.getName()).append(",");
            stringBuilder.append(v.getX()).append(",");
            stringBuilder.append(v.getY()).append(",");
            stringBuilder.append(v.getCreationDate()).append(",");
            stringBuilder.append(v.getCapacity()).append(",");
            stringBuilder.append(v.getEnginePower()).append(",");
            stringBuilder.append(v.getFuelType()).append(",");
            stringBuilder.append(v.getVehicleType()).append(",");
            stringBuilder.append(v.getUser()).append(",");
        }
        System.out.println(stringBuilder);
        return stringBuilder.toString();
    }
    private <T> T deserialize(ByteBuffer byteBuffer) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteBuffer.array());
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        T data;
        try {
            data = (T) objectInputStream.readObject();
        } catch (IOException e) {
            data = null;
        }
        byteArrayInputStream.close();
        objectInputStream.close();
        byteBuffer.clear();
        return data;
    }

    private DataToOutput<?> read(ByteBuffer buffer) {
        try {
            clientDialog.read(buffer);
            DataToOutput<?> command = deserialize(buffer);
            buffer.clear();
            return command;
        } catch (IOException | ClassNotFoundException e) {
            try {
                clientDialog.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
        return null;
    }
}
