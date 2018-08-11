package bpTree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * ファイルの出し入れをまとめたクラス
 */
public class FileIO {

    public static void write(String fileName, Object target) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(fileName);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(target);
        }catch(IOException ioe) {
//            ioe.printStackTrace();
        }finally {
            try {
                oos.flush();
            } catch (IOException e) {
//                e.printStackTrace();
            }
            try {
                oos.close();
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
    }
    public static Object read(String fileName) {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        Object retObject = null;
        try {
            fis = new FileInputStream(fileName);
            ois = new ObjectInputStream(fis);
            retObject = ois.readObject();
        }catch(IOException ioe) {
//            ioe.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
//            cnfe.printStackTrace();
        }
        return retObject;
    }


    public static void fileDelete(String filename) {
        File file = new File(filename);

        if(file.exists()){
        	if(!file.delete()){
        		System.out.println("ファイルの削除に失敗しました");
        	}
        }
    }
}
