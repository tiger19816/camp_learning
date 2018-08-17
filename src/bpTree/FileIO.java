package bpTree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * ファイルの出し入れをまとめたクラス
 */
public class FileIO {

	/**
	 * ファイルへオブジェクトの書き込み
	 * @param fileName ファイル名
	 * @param target 書き込むオブジェクト
	 */
    public static void write(String filename, Object target) throws Exception {
    	File tempFile = null;
		Path tmpPath = Files.createTempFile(Paths.get("./"), "temp", ".dat");
		tempFile = tmpPath.toFile();

    	//新しいファイルの作成
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;

        fos = new FileOutputStream(tempFile);
        oos = new ObjectOutputStream(fos);
        oos.writeObject(target);
        oos.flush();

		//ファイルを永続化
		fos.getFD().sync();

        oos.close();
		fos.close();

		//ファイルのリネーム
        FileSystem fs = FileSystems.getDefault();
        Path newFile = fs.getPath(filename);
        Files.move(tmpPath, newFile, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * ファイルからオブジェクトを読み込む
     * @param fileName ファイル名
     * @return 読み込んだオブジェクト
     */
    public static Object read(String filename) {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        Object retObject = null;
        try {
            fis = new FileInputStream(filename);
            ois = new ObjectInputStream(fis);
            retObject = ois.readObject();
        } catch(IOException ioe) {
//            ioe.printStackTrace();
        } catch(ClassNotFoundException cnfe) {
//            cnfe.printStackTrace();
        } finally {
			if(ois != null) {
				try {
					ois.close();
				} catch (IOException e) {
				}
			}
			if(fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
        return retObject;
    }

    /**
     * ファイルを削除する
     * @param filename 削除するファイル名
     */
    public static void fileDelete(String filename) {
        File file = new File(filename);

        if(file.exists()){
        	if(!file.delete()){
        		System.out.println("ファイルの削除に失敗しました");
        	}
        }
    }

	/**
	 * ファイルを中身を空にする
	 * @param fileName ファイル名
	 */
    public static void clearFile(String filename) throws Exception {
    	File tempFile = null;
		Path tmpPath = Files.createTempFile(Paths.get("./"), "temp", ".dat");
		tempFile = tmpPath.toFile();

    	//新しいファイルの作成
        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.flush();

		//ファイルを永続化
		fos.getFD().sync();

		fos.close();

		//ファイルのリネーム
        FileSystem fs = FileSystems.getDefault();
        Path newFile = fs.getPath(filename);
        Files.move(tmpPath, newFile, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * ファイルの存在を確認する
     * @param filename 確認するファイル名
     * @return ファイルがあれば true、なければ false
     */
    public static boolean fileExists(String filename) {
        File file = new File(filename);

        if(file.exists()){
        	return true;
        } else {
        	return false;
        }
    }
}
