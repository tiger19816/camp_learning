package bpTree;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * BPMapにつなぐクラス
 */
public class Connection {

	private BPMap<Integer, Values> m;
	private BPMap<Integer, Object> commands;
    /**
     * 書き込まれた回数(初期ポインタの位置)を保存するフィールド
     */
    private int writeCnt = 0;

    /**
     * コンストラクタ
     * @param m
     */
	public Connection(BPMap<Integer, Values> m) {
		this.m = m;
		commands = new BPMap<>();
	}

	/**
	 * insertを行う
	 * @param key
	 * @param value1
	 * @param value2
	 */
	public boolean insert(int key, String value1, String value2) {
//		if(commands.member(key)) {//ライトセットに key があるか
//			Object command = commands.lookup(key);
//			if(command instanceof InsertCommand || command instanceof UpdateCommand) {//指定した key に insert が行われていた場合
//				System.out.println("入力されたキーは既に存在しています");
//				return false;
//			} else if(command instanceof DeleteCommand) {//指定した key に delete が行われていた場合
//				commands.insert(key, new InsertCommand(key, value1, value2));
//				return true;
//			}
//		} else if(m.root != m.getNil()) {//DBにデータが存在しているか
//			if(m.member(key)) {//DB に key があるか
//				System.out.println("入力されたキーは既に存在しています");
//				return false;
//			} else {
//				commands.insert(key, new InsertCommand(key, value1, value2));
//				return true;
//			}
//		} else {
//			commands.insert(key, new InsertCommand(key, value1, value2));
//			return true;
//		}
//		return false;

		if(m.root != m.getNil()) {
			if(m.member(key)) {
				if(commands.member(key)) {//ライトセットに key があるか
					Object command = commands.lookup(key);
					if(command instanceof DeleteCommand) {//指定した key に delete が行われていた場合
						commands.insert(key, new UpdateCommand(key, value1, value2));
						return true;
					}
				}
			} else {
				if(commands.member(key)) {//ライトセットに key があるか
					Object command = commands.lookup(key);
					if(command instanceof DeleteCommand) {//指定した key に delete が行われていた場合
						commands.insert(key, new InsertCommand(key, value1, value2));
						return true;
					}
				} else {
					commands.insert(key, new InsertCommand(key, value1, value2));
					return true;
				}
			}
		} else {
			if(commands.member(key)) {//ライトセットに key があるか
				Object command = commands.lookup(key);
				if(command instanceof DeleteCommand) {//指定した key に delete が行われていた場合
					commands.insert(key, new InsertCommand(key, value1, value2));
					return true;
				}
			} else {
				commands.insert(key, new InsertCommand(key, value1, value2));
				return true;
			}
		}
		System.out.println("1入力されたキーは既に存在しています：key=" + key);
		return false;
	}

	/**
	 * update を行う
	 */
	public boolean update(int key, String value1, String value2) {
		if(commands.member(key)) {//ライトセットに key があるか
			Object command = commands.lookup(key);
			if(command instanceof InsertCommand) {//指定した key に insert が行われていた場合
				commands.insert(key, new InsertCommand(key, value1, value2));
				return true;
			} else if(command instanceof UpdateCommand) {//指定した key に update が行われていた場合
				if(m.root != m.getNil()) {//DBにデータが存在しているか
					if(m.member(key)) {//DB に key があるか
						commands.insert(key, new UpdateCommand(key, value1, value2));
						return true;
					}
				}
			}
		} else if(m.root != m.getNil()) {//DBにデータが存在しているか
			if(m.member(key)) {//DB に key があるか
				commands.insert(key, new UpdateCommand(key, value1, value2));
				return true;
			}
		}
		System.out.println("2入力されたキーは存在しません：key=" + key);
		return false;
	}

	/**
	 * deleteを行う
	 * @param key
	 * @return
	 */
	public boolean delete(int key) {
		if(m.root != m.getNil()) {//DBにデータが存在しているか
			if(m.member(key)) {
				if(commands.member(key)) {//ライトセットに key があるか
					Object command = commands.lookup(key);
					if(command instanceof DeleteCommand) {//指定した key に delete が行われていた場合
						System.out.println("3入力されたキーは存在しません：key=" + key);
						return false;
					}
				}
				commands.insert(key, new DeleteCommand(key));
				return true;
			} else {
				if(commands.member(key)) {//ライトセットに key があるか
					Object command = commands.lookup(key);
					if(command instanceof InsertCommand || command instanceof UpdateCommand) {//指定した key に delete が行われていた場合
						commands.delete(key);
						return true;
					}
				}
			}
		} else {
			if(commands.member(key)) {//ライトセットに key があるか
				Object command = commands.lookup(key);
				if(command instanceof InsertCommand || command instanceof UpdateCommand) {//指定した key に delete が行われていた場合
					commands.delete(key);
					return true;
				}
			}
		}
		System.out.println("4入力されたキーは存在しません：key=" + key);
		return false;
	}

	/**
	 * トランザクションのコミットを行う
	 */
	public void commit() {
		BPMap<?, ?>.NodeBottom nb = commands.minNode();
		while(nb != commands.getNil()) {
			for(int i = 0; i < nb.ks().size(); i++) {
				Object command = nb.getVs().get(i);
				if(command instanceof InsertCommand) {
					InsertCommand ic = (InsertCommand) command;
					write("insert", ic.getKey() + "", ic.getValue1(), ic.getValue2());
					m.insert(ic.getKey(), new Values(ic.getValue1(), ic.getValue2()));
				} else if(command instanceof UpdateCommand) {
					UpdateCommand uc = (UpdateCommand) command;
					write("update", uc.getKey() + "", uc.getValue1(), uc.getValue2());
					m.update(uc.getKey(), new Values(uc.getValue1(), uc.getValue2()));
				} else if(command instanceof DeleteCommand) {
					DeleteCommand dc = (DeleteCommand) command;
					write("delete", dc.getKey() + "");
					m.delete(dc.getKey());
//					System.out.println(m);
				}
			}
			nb = nb.getNext();
		}
		write("commit");
		commands = new BPMap<>();
	}

	/**
	 * トランザクションの破棄
	 */
	public void abort() {
		write("abort");
		commands = new BPMap<>();
	}

	/**
	 * select を行う
	 */
	public void select(int key) {
		if(commands.member(key)) {
			Object command = commands.lookup(key);
			if(command instanceof InsertCommand) {
				InsertCommand ic = (InsertCommand) command;
				System.out.print("値1：" + ic.getValue1());
				System.out.println("   |値2：" + ic.getValue2());
			} else if(command instanceof UpdateCommand) {
				UpdateCommand uc = (UpdateCommand) command;
				System.out.print("値1：" + uc.getValue1());
				System.out.println("   |値2：" + uc.getValue2());
			} else if(command instanceof DeleteCommand) {
				System.out.println("入力されたキーは存在しません");
			}
		} else if(m.member(key)) {
        	Values values = m.lookup(key);
			System.out.print("値1：" + values.getStr());
			System.out.println("   |値2：" + values.getNum());
		} else {
			System.out.println("入力されたキーは存在しません");
		}
	}

    /**
     * REDOログの書き込み
     * @param values 処理内容
     */
    private void write(String... values){
        //ファイルの作成
    	RandomAccessFile randomfile = null;
        try {
            randomfile =  new RandomAccessFile(Main.REDO_FILENAME,"rw");
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

    private class Command {
    	protected int key;
    	protected String value1;
    	protected String value2;

    	public Command(int key, String value1, String value2) {
    		this.key = key;
    		this.value1 = value1;
    		this.value2 = value2;
    	}

		public int getKey() {
			return key;
		}
		public void setKey(int key) {
			this.key = key;
		}
		public String getValue1() {
			return value1;
		}
		public void setValue1(String value1) {
			this.value1 = value1;
		}
		public String getValue2() {
			return value2;
		}
		public void setValue2(String value2) {
			this.value2 = value2;
		}
    }

    private class InsertCommand extends Command {
    	public InsertCommand(int key, String value1, String value2) {
    		super(key, value1, value2);
    	}
    }

    private class UpdateCommand extends Command {
    	public UpdateCommand(int key, String value1, String value2) {
    		super(key, value1, value2);
    	}
    }

    private class DeleteCommand {
    	private int key;

    	public DeleteCommand(int key) {
    		this.key = key;
    	}

		public int getKey() {
			return key;
		}

		public void setKey(int key) {
			this.key = key;
		}
    }
}
