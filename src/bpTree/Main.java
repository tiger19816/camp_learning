package bpTree;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
	/**
	 * データを保存するファイル名
	 */
	private static final String DATA_FILENAME = "data.dat";
	/**
	 * REDOデータを保存するファイル名
	 */
	private static final String REDO_FILENAME = "redo.dat";
    /**
     * 書き込まれた回数(初期ポインタの位置)を保存するフィールド
     */
    private static int writeCnt = 0;

    public static void main(String[] args) {
    	//データを読み込む
        BPMap<Integer, Values> m = (BPMap<Integer, Values>) FileIO.read(DATA_FILENAME);

        //データがない場合はnewする
        if(m == null) {
        	m = new BPMap<>();
        }

        //REDOする処理
        RandomAccessFile randomfile = null;
        try {
            //RandomAccessFileオブジェクトの生成
            randomfile = new RandomAccessFile(REDO_FILENAME, "rw");

            int pointer = 0;

            while(pointer < randomfile.length()) {
                randomfile.seek(pointer);
                String log = randomfile.readLine();
                if(log != null){
                	switch (log) {
					case "insert":
						pointer += 12;
						randomfile.seek(pointer);
						int key = Integer.valueOf(randomfile.readLine());
						pointer += 12;
						randomfile.seek(pointer);
						String value1 = randomfile.readLine();
						pointer += 12;
						randomfile.seek(pointer);
						String value2 = randomfile.readLine();
						m.insert(key, new Values(value1, value2));
						break;
					case "delete":
						pointer += 12;
						randomfile.seek(pointer);
						int deleteKey = Integer.valueOf(randomfile.readLine());
						if(m.member(deleteKey)) {
							m.delete(deleteKey);
						}
						break;
					default:
						break;
					}
					pointer += 12;
                }
            }
        } catch (IOException e) {
        } finally {
        	if(randomfile != null) {
        		try {
                	randomfile.close();  //RandomAccessFileストリームのクローズ
        		} catch (IOException e) {
				}
        	}
		}

        //データを保存してWALを削除
        FileIO.write(DATA_FILENAME, m);
        FileIO.fileDelete(REDO_FILENAME);

		System.out.println("コマンド一覧");
		System.out.println("insert キー(Integer) 値1(String) 値2(Integer)");
		System.out.println("delete キー(Integer)");
		System.out.println("select キー(Integer)");
		System.out.println("range キー(Integer) 範囲(Integer)");
		System.out.println("tree(木構造確認)");
		System.out.println("end(終了)");
		System.out.println();

		//コンソールからキーボード入力を受けるオブジェクト
    	Scanner scan = new Scanner(System.in);

    	boolean flag = true;

		//endされるまで処理を回し続ける
    	while(flag) {
    		try {
	    		System.out.print("コマンドを入力＞");
	    		String command = scan.next();

	    		//処理の分岐
	    		switch (command) {
				case "insert":
		        	int insertKey = scan.nextInt();
		        	String value1 = scan.next();
		        	String value2 = scan.next();
			        m.insert(insertKey, new Values(value1, value2));
			        write("insert", insertKey + "", value1, value2);
					break;
				case "select":
					try {
			        	int key = scan.nextInt();
			        	Values values = m.lookup(key);
						System.out.println("値1：" + values.getStr());
						System.out.println("値2：" + values.getNum());
					} catch(NullPointerException ex) {
						System.out.println("入力されたキーは存在しません");
					}
					break;
				case "delete":
		        	int deleteKey = scan.nextInt();
					if(m.member(deleteKey)) {
						m.delete(deleteKey);
						write("delete", deleteKey + "");
					} else {
						System.out.println("入力されたキーは存在しません");
					}
					break;
				case "tree":
					if(!m.isEmpty()) {
						System.out.println(m);
						System.out.println();
				        System.out.println("size: " + m.size());
				        System.out.println("keys: " + m.keys());
				        System.out.println("values: " + m.values());
					} else {
						System.out.println("空です");
					}
					break;
				case "range":
					int rangeKey = scan.nextInt();
					int rangeNum = scan.nextInt();
					ArrayList<Values> range = m.range(rangeKey, rangeNum);
					for(Values values : range) {
						System.out.println(values.getStr() + " " + values.getNum());
					}
					break;
				case "end":
					flag = false;
			        FileIO.write(DATA_FILENAME, m);
			        FileIO.fileDelete(REDO_FILENAME);
					break;
				default:
					System.out.println("入力されたコマンドは存在しません");
					System.out.println("コマンド一覧");
					System.out.println("insert キー(Integer) 値1(String) 値2(Integer)");
					System.out.println("delete キー(Integer)");
					System.out.println("select キー(Integer)");
					System.out.println("range キー(Integer) 範囲(Integer)");
					System.out.println("tree(木構造確認)");
					System.out.println("end(終了)");
					break;
				}
    		} catch (Exception e) {
    			System.out.println("エラー発生");
			}
    	}
    }

    /**
     * REDOログの書き込み
     * @param values 処理内容
     */
    public static void write(String... values){
        //ファイルの作成
    	RandomAccessFile randomfile = null;
        try {
            randomfile =  new RandomAccessFile(REDO_FILENAME,"rw");
            int i;
            for (i = 0; i < values.length; i++){
            	//ファイルポインタの設定
                randomfile.seek((writeCnt + i) * 12);
       	    	randomfile.writeBytes(values[i] + "\n");
            }
            writeCnt += i;
        } catch (IOException e) {
        } finally {
        	if(randomfile != null) {
        		try {
        			//RandomAccessFileストリームのクローズ
                	randomfile.close();
        		} catch (IOException e) {
				}
        	}
		}
    }
}
