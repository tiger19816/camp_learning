package bpTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BPMap<Key extends Comparable<? super Key>, Value> {

	/**
	 * B+木のオーダー
	 */
	private static final int m = 5;
	/**
	 * B+木の最小分岐数[m/2]
	 */
	private static final int hm = (m + 1) / 2;

	/**
	 * ノードの型（抽象型）
	 */
	private abstract class Node {
		/**
		 * キーのリスト
		 */
		public List<Key> ks() {
			return null;
		}

		/**
		 * 枝のリスト
		 */
		public List<Node> ns() {
			return null;
		}

		public Node deactivate() {
			return this;
		}

		public Node trim() {
			return this;
		}

		public Node insert(Key key, Value value) {
			return this;
		}

		public void delete(Key key) {}

		public Key deleteMin() {
			return null;
		}

		public void balanceLeft(NodeInterior t, int i) {}

		public void balanceRight(NodeInterior t, int j) {}
	}

	/**
	 * nullの代わり。
	 */
	private final NodeBottom nil = new NodeBottom();

	/**
	 * B+木の根(nilは空の木)
	 */
	private Node root = nil;

	/**
	 * 挿入時のアクティブなノードか判定する
	 * @param t Node
	 * @return 挿入時のアクティブなノードならtrue
	 */
	private boolean active(Node t) {
		return t instanceof BPMap<?, ?>.NodeActive;
	}

	/**
	 * 最下層のノードか判定する
	 * @param t Node
	 * @return 最下層のノードならtrue
	 */
	private boolean bottom(Node t) {
		return t instanceof BPMap<?, ?>.NodeBottom;
	}

	/**
	 * 挿入時のアクティブなノードの型
	 */
	private class NodeActive extends Node {
		/**
		 * キーのリスト
		 */
		private List<Key> ks = new ArrayList<Key>(1);
		/**
		 * 枝のリスト
		 */
		private List<Node> ns = new ArrayList<Node>(2);

		/**
		 * コンストラクタ
		 */
		public NodeActive(Key key, Node l, Node r) {
			ks.add(key);
			ns.add(l);
			ns.add(r);
		}

		@Override
		public List<Key> ks() {
			return ks;
		}

		@Override
		public List<Node> ns() {
			return ns;
		}

		/**
		 * アクティブなノードを内部ノードに変換する
		 */
		@Override
		public Node deactivate() {
			return new NodeInterior(ks.get(0), ns.get(0), ns.get(1));
		}
	}

	/**
	 * 内部ノードの型
	 */
	private class NodeInterior extends Node {
		/**
		 * キーのリスト
		 */
		private List<Key> ks = new ArrayList<Key>(m);
		/**
		 * 枝のリスト
		 */
		private List<Node> ns = new ArrayList<Node>(m + 1);

		/**
		 * コンストラクタ
		 */
		public NodeInterior() {}

		/**
		 * コンストラクタ
		 */
		public NodeInterior(Key key, Node l, Node r) {
			ks.add(key);
			ns.add(l);
			ns.add(r);
		}

		@Override
		public List<Key> ks() {
			return ks;
		}

		@Override
		public List<Node> ns() {
			return ns;
		}

		/**
		 * 枝が一本の余分なノードを切り詰める
		 */
		@Override
		public Node trim() {
			return ns.size() == 1 ? ns.get(0) : this;
		}

        //=====================================================================
        // 内部ノードでの挿入
        //=====================================================================

		/**
		 * 木 this にキー key で value を挿入する
		 */
		@Override
		public Node insert(Key key, Value value) {
			int i;
			for(i = 0; i < ks.size(); i++) {
				int compare = key.compareTo(ks.get(i));
				if(compare < 0) {
					ns.set(i, ns.get(i).insert(key, value));
					return balance(i);
				} else if(compare == 0) {
					ns.set(i + 1, ns.get(i + 1).insert(key, value));
					return balance(i + 1);
				}
			}
			ns.set(i, ns.get(i).insert(key, value));
			return balance(i);
		}

		/**
		 * 挿入時のバランス調整
		 */
		public Node balance(int i) {
			Node ni = ns.get(i);
			if(!active(ni)) {
				return this;
			}
			ks.add(i, ni.ks().get(0));
			ns.set(i, ni.ns().get(1));
			ns.add(i, ni.ns().get(0));
			return ks.size() < m ? this : split();
		}

		/**
		 * 要素数がmのノードを分割してアクティブなノードに変換する
		 */
		public Node split() {
			int j = hm;
			int i = j -1;
			NodeInterior l = this;
			NodeInterior r = new NodeInterior();
			r.ks.addAll(l.ks.subList(j, m));
			r.ns.addAll(l.ns.subList(j, m + 1));
			l.ks.subList(j, m).clear();
			l.ns.subList(j, m + 1).clear();
			return new NodeActive(l.ks.remove(i), l, r);
		}

        //=====================================================================
        // 内部ノードでの削除
        //=====================================================================

		/**
		 * 木 this からキー key のノードを削除する
		 */
		@Override
		public void delete(Key key) {
			int i;
			for(i = 0; i < ks.size(); i++) {
				int compare = key.compareTo(ks.get(i));
				if(compare < 0) {
					ns.get(i).delete(key);
					ns.get(i).balanceLeft(this, i);
					return;
				} else if(compare == 0) {
					ks.set(i, ns.get(i + 1).deleteMin());
					ns.get(i + 1).balanceRight(this, i + 1);
				}
				ns.get(i).delete(key);
				ns.get(i).balanceRight(this, i);
			}
		}

		/**
		 * 部分木 this の最小値キーを削除する
		 * @return 部分木 this の当たらな最小値キー
		 */
		@Override
		public Key deleteMin() {
			Key nmin = ns.get(0).deleteMin();
			Key spare = ks.get(0);
			ns.get(0).balanceLeft(this, 0);
			return nmin != null ? nmin : spare;
		}

		/**
		 * 左部分木での削除時のバランス調整
		 */
		@Override
		public void balanceLeft(NodeInterior t, int i) {
			NodeInterior ni = this;
			if(ni.ns.size() >= hm) {
				return;
			}
			//以下、ni がアクティブな場合
			int j = i + 1;
			Key key = t.ks.get(i);
			NodeInterior nj = (NodeInterior) t.ns.get(j);
			if(nj.ns.size() == hm) {
				ni.ks.add(key);
				ni.ks.addAll(nj.ks);
				ni.ns.addAll(nj.ns);
				t.ks.remove(i);
				t.ns.remove(j);
			} else {
				t.ks.set(i, moveRL(key, ni, nj));
			}
		}

		/**
		 * 右部分木での削除時のバランス調整
		 */
		@Override
		public void balanceRight(NodeInterior t, int j) {
			NodeInterior nj = this;
			if(nj.ns.size() >= hm) {
				return;
			}
			// 以下、nj がアクティブの場合
			int i = j - 1;
			Key key = t.ks.get(i);
			NodeInterior ni = (NodeInterior) t.ns.get(j);
			if(ni.ns.size() == hm) {
				ni.ks.add(key);
				ni.ks.addAll(nj.ks);
				ni.ns.addAll(nj.ns);
				t.ks.remove(i);
				t.ns.remove(j);
			} else {
				t.ks.set(i, moveLR(key, ni, nj));
			}
		}

		/**
		 * 余裕のある右ノードから枝を1本分けてもらう
		 */
		public Key moveRL(Key key, NodeInterior l, NodeInterior r) {
			l.ks.add(key);
			l.ns.add(r.ns.remove(0));
			return r.ks.remove(0);
		}

		/**
		 * 余裕のある左ノードから枝を1本分けてもらう
		 */
		public Key moveLR(Key key, NodeInterior l, NodeInterior r) {
			int j = l.ks.size();
			int i = j - 1;
			r.ks.add(0, key);
			r.ns.add(0, l.ns.remove(j));
			return l.ks.remove(i);
		}
	}

	/**
	 * 最下層のノードの型
	 */
	private class NodeBottom extends Node {
		/**
		 * キーのリスト
		 */
		private List<Key> ks = new ArrayList<Key>(m);
		/**
		 * 値のリスト
		 */
		private List<Value> vs = new ArrayList<Value>(m);
		/**
		 * 右隣の最下層のノード
		 */
		private NodeBottom next = nil;

		/**
		 * コンストラクタ
		 */
		public NodeBottom() {}

		/**
		 * コンストラクタ
		 */
		public NodeBottom(Key key, Value value) {
			ks.add(key);
			vs.add(value);
		}

		@Override
		public List<Key> ks() {
			return ks;
		}

		/**
		 * キーも枝もないノードを空の木(nil)に変換する
		 */
		@Override
		public Node trim() {
			return ks.size() == 0 ? nil : this;
		}

        //=====================================================================
        // 最下層のノードでの挿入
        //=====================================================================

		/**
		 * 木 this にキー key で値 x を挿入する
		 */
		@Override
		public Node insert(Key key, Value value) {
			if(this == nil) {
				return new NodeBottom(key, value);
			}
			int i;
			for(i = 0; i < ks.size(); i++) {
				int compare = key.compareTo(ks.get(i));
				if(compare < 0) {
					return balance(i, key, value);
				} else if(compare == 0) {
					vs.set(i, value);
					return this;
				}
			}
			return balance(i, key, value);
		}

		/**
		 * 挿入時のバランス調整
		 */
		public Node balance(int i, Key key, Value value) {
			ks.add(i, key);
			vs.add(i, value);
			return ks.size() < m ? this : split();
		}

		/**
		 * 要素数が m のノードを分割してアクティブなノードに変換する
		 */
		public Node split() {
			int j = hm - 1;
			NodeBottom l = this;
			NodeBottom r = new NodeBottom();
			r.ks.addAll(l.ks.subList(j, m));
			r.vs.addAll(l.vs.subList(j, m));
			l.ks.subList(j, m).clear();
			l.vs.subList(j, m).clear();
			r.next = l.next;
			l.next = r;
			return new NodeActive(r.ks.get(0), l, r);
		}

        //=====================================================================
        // 最下層のノードでの削除
        //=====================================================================

		/**
		 * 木 this からキー key のノードを削除する
		 */
		@Override
		public void delete(Key key) {
			for(int i = 0; i < ks.size(); i++) {
				int compare = key.compareTo(ks.get(i));
				if(compare < 0) {
					return;
				} else if(compare == 0) {
					ks.remove(i);
					vs.remove(i);
					return;
				}
			}
		}

		/**
		 * 部分木 this の最小値キーを削除する
		 * @return 部分木 this の新たな最小値キーを返す。空になったら null を返す
		 */
		@Override
		public Key deleteMin() {
			ks.remove(0);
			vs.remove(0);
			return !ks.isEmpty() ? ks.get(0) : null;
		}

		/**
		 * 左部分木での削除時のバランス調整
		 */
		@Override
		public void balanceLeft(NodeInterior t, int i) {
			NodeBottom ni = this;
			if(ni.ks.size() >= hm - 1) {
				return;
			}
			int j = i + 1;
			NodeBottom nj = (NodeBottom) t.ns.get(j);
			if(nj.ks.size() == hm - 1) {
				ni.ks.addAll(nj.ks);
				ni.vs.addAll(nj.vs);
				t.ks.remove(i);
				t.ns.remove(j);
				ni.next = nj.next;
			} else {
				t.ks.set(i, moveRL(ni, nj));
			}
		}

		/**
		 * 右部分木での削除時のバランス調整
		 */
		@Override
		public void balanceRight(NodeInterior t, int j) {
			NodeBottom nj = this;
			if(nj.ks.size() >= hm - 1) {
				return;
			}
			int i = j - 1;
			NodeBottom ni = (NodeBottom) t.ns.get(j);
			if(ni.ks.size() == hm - 1) {
				ni.ks.addAll(nj.ks);
				ni.vs.addAll(nj.vs);
				t.ks.remove(i);
				t.ns.remove(j);
				ni.next = nj.next;
			} else {
				t.ks.set(i, moveLR(ni, nj));
			}
		}

		/**
		 * 余裕のある右ノードから枝を1本分けてもらう
		 */
		public Key moveRL(NodeBottom l, NodeBottom r) {
			l.ks.add(r.ks.remove(0));
			l.vs.add(r.vs.remove(0));
			return r.ks.get(0);
		}

		/**
		 * 余裕のある左ノードから枝を1本分けてもらう
		 */
		public Key moveLR(NodeBottom l, NodeBottom r) {
			int i = l.ks.size() - 1;
			r.ks.add(0, l.ks.remove(i));
			r.vs.add(0, l.vs.remove(i));
			return r.ks.get(0);
		}
	}

	/**
	 * 木 root にキー key で値 x を挿入する
	 */
	public void insert(Key key, Value value) {
		root = root.insert(key, value).deactivate();
	}

	/**
	 * 木 root からキー key のノードを削除する
	 */
	public void delete(Key key) {
		root.delete(key);
		root = root.trim();
	}

	/**
	 * キーの検索
	 * @return ヒットすれば true、しなければ false
	 */
	public boolean member(Key key) {
		if(root == nil) {
			return false;
		}
		Node t = root;
		while(!bottom(t)) {
			int i;
			for(i = 0; i < t.ks().size(); i++) {
				final int compare = key.compareTo(t.ks().get(i));
				if(compare < 0) {
					break;
				} else if(compare == 0) {
					return true;
				}
			}
			t = t.ns().get(i);
		}
		NodeBottom u = (NodeBottom) t;
		for(int i = 0; i < u.ks.size(); i++) {
			if(key.compareTo(u.ks.get(i)) == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * キーから値を得る。
	 * @return キーから得た値。ヒットしない場合 null を返す
	 */
	public Value lookup(Key key) {
		if(root == nil) {
			return null;
		}
		Node t = root;
		while(!bottom(t)) {
			int i;
			for(i = 0; i < t.ks().size(); i++) {
				final int compare = key.compareTo(t.ks().get(i));
				if(compare < 0) {
					break;
				} else if(compare == 0) {
					i++;
					break;
				}
			}
			t = t.ns().get(i);
		}
		NodeBottom u = (NodeBottom) t;
		for(int i = 0; i < u.ks.size(); i++) {
			if(key.compareTo(u.ks.get(i)) == 0) {
				return u.vs.get(i);
			}
		}
		return null;
	}

	public ArrayList<Value> range(Key key, int range) {
		if(root == nil) {
			return null;
		}
		Node t = root;
		while(!bottom(t)) {
			int i;
			for(i = 0; i < t.ks().size(); i++) {
				final int compare = key.compareTo(t.ks().get(i));
				if(compare < 0) {
					break;
				} else if(compare == 0) {
					i++;
					break;
				}
			}
			t = t.ns().get(i);
		}
		NodeBottom u = (NodeBottom) t;
		ArrayList<Value> result = new ArrayList<Value>();
		for(int i = 0; i < u.ks.size(); i++) {
			if(key.compareTo(u.ks.get(i)) == 0) {
				while(i < u.ks.size() && 0 < range) {
					result.add(u.vs.get(i));
					range--;
					i++;
					if(i == u.ks.size() && 0 < range) {
						u = u.next;
						if(u == nil) {
							break;
						}
						i = 0;
					} else if(range == 0) {
						break;
					}
				}
				break;
			}
		}
		return result;
	}

	/**
	 * マップが空かどうか
	 * @return マップが空なら true、空でないなら false
	 */
	public boolean isEmpty() {
		return root == nil;
	}

	/**
	 * マップを空にする
	 */
	public void clear() {
		root = nil;
	}

	/**
	 * B+木のキーリストを返す
	 * @return B+木のキーのリスト
	 */
	public ArrayList<Key> keys() {
		if(root == nil) {
			return null;
		}
		Node t = root;
		while(!bottom(t)) {
			t = t.ns().get(0);
		}
		NodeBottom u = (NodeBottom) t;
		ArrayList<Key> al = new ArrayList<Key>();
		while(u != nil) {
			al.addAll(u.ks);
			u = u.next;
		}
		return al;
	}

	/**
	 * B+木の値のリストを返す
	 * @return B+木の値の値のリスト
	 */
	public ArrayList<Value> values() {
		if(root == nil) {
			return null;
		}
		Node t = root;
		while(!bottom(t)) {
			t = t.ns().get(0);
		}
		NodeBottom u = (NodeBottom) t;
		ArrayList<Value> al = new ArrayList<Value>();
		while(u != nil) {
			al.addAll(u.vs);
			u = u.next;
		}
		return al;
	}

	/**
	 * マップのサイズを返す
	 * @return マップのサイズ
	 */
	public int size() {
		return keys().size();
	}

	/**
	 * キーの最小値を返す
	 * @return キーの最小値
	 */
	public Key min() {
		if(root == nil) {
			return null;
		}
		Node t = root;
		while(!bottom(t)) {
			t = t.ns().get(0);
		}
		return t.ks().get(0);
	}

	/**
	 * キーの最大値を返す
	 * @return キーの最大値
	 */
	public Key max() {
		if(root == nil) {
			return null;
		}
		Node t = root;
		while(!bottom(t)) {
			t = t.ns().get(t.ns().size() - 1);
		}
		return t.ks().get(t.ks().size() - 1);
	}

    //=========================================================================
    // メインルーチン
    //=========================================================================

    public static void main(String[] args) {

    	Scanner scan = new Scanner(System.in);

    	boolean flag = true;

    	BPMap<Integer, Integer> m = new BPMap<>();

    	while(flag) {

    		System.out.print("コマンドを入力：");
    		String command = scan.next();

    		switch (command) {
			case "insert":
				System.out.println("insertモード");
				while(true) {
					System.out.println("keyとvalueを入力(keyを-1でコマンド入力に戻る)");
		        	int key = scan.nextInt();
		        	if(key == -1) {
			        	break;
		        	}
		        	int value = scan.nextInt();
			        m.insert(key, value);
				}
				break;
			case "select":
				try {
					System.out.print("値を取り出したいキーを入力：");
		        	int key = scan.nextInt();
		        	int value = m.lookup(key);
					System.out.println("値：" + value);
				} catch(NullPointerException ex) {
					System.out.println("入力されたキーは存在しません");
				}
				break;
			case "delete":
				System.out.print("削除したいキーを入力：");
	        	int key = scan.nextInt();
				if(m.member(key)) {
					m.delete(key);
				} else {
					System.out.println("キーが存在しません");
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
				ArrayList<Integer> range = m.range(rangeKey, 3);
				for(Integer value : range) {
					System.out.println(value);
				}
				break;
			case "end":
				flag = false;
				break;
			default:
				System.out.println("入力されたコマンドは存在しません");
				System.out.println("insert");
				System.out.println("select");
				System.out.println("delete");
				System.out.println("tree");
				System.out.println("end");
				break;
			}
    	}

//        final int n = 100;
//        BPMap<Integer,Integer> m = new BPMap<>();
//        ArrayList<Integer> keys = new ArrayList<>();
//        for (int i = 0; i < n; i++) keys.add(i);
//        java.util.Collections.shuffle(keys);
//        for (int i = 0; i < n; i++) m.insert(keys.get(i), i);
//        System.out.println(m);
//        System.out.println();
//        System.out.println("size: " + m.size());
//        System.out.println("keys: " + m.keys());
//        System.out.println("values: " + m.values());
//
//        final int N = 1000000;
//        java.util.Random rng = new java.util.Random();
//        m.clear();
//        java.util.TreeMap<Integer,Integer> answer = new java.util.TreeMap<>();
//        int insertErrors = 0;
//        int deleteErrors = 0;
//        for (int i = 0; i < N; i++) {
//            int key = rng.nextInt(N);
//            m.insert(key, i);
//            answer.put(key, i);
//        }
//        for (int key: answer.keySet()) {
//            int x = m.lookup(key);
//            int y = answer.get(key);
//            if (x != y) insertErrors++;
//        }
//        int half = answer.size()/2;
//        for (int key: answer.keySet()) {
//            if (half-- == 0) break;
//            m.delete(key);
//        }
//        half = answer.size()/2;
//        for (int key: answer.keySet()) {
//            if (half-- == 0) break;
//            if (m.member(key)) deleteErrors++;
//        }
//        System.out.println();
//        System.out.println("バランス:   " + (m.balanced()      ? "OK" : "NG"));
//        System.out.println("多分探索木: " + (m.mstValid()      ? "OK" : "NG"));
//        System.out.println("密度:       " + (m.dense()         ? "OK" : "NG"));
//        System.out.println("挿入:       " + (insertErrors == 0 ? "OK" : "NG"));
//        System.out.println("削除:       " + (deleteErrors == 0 ? "OK" : "NG"));
//        for (int key: m.keys()) m.delete(key);
//        System.out.println("全削除:     " + (m.isEmpty()       ? "OK" : "NG"));
    }


    //=========================================================================
    // デバッグ用ルーチン
    //=========================================================================

    // B+木をグラフ文字列に変換する
    public String toString() {
        return toGraph("", root).replaceAll("\\s+$", "");
    }
//
//    // バランスが取れているか確認する
//    public boolean balanced() { return balanced(root); }
//
//    // 多分探索木になっているか確認する
//    public boolean mstValid() { return mstValid(root); }
//
//    // 根と最下層のノードを除くノードが hm 以上の枝を持っているか確認する
//    public boolean dense() {
//        if (root == nil) return true;
//        int n = root.ns().size();
//        if (bottom(root)) { if (n < 1 || m-1 < n) return false; }
//        else {
//            if (n < 2 || m < n) return false;
//            for (int i = 0; i < n; i++)
//                if (!denseHalf(root.ns().get(i))) return false;
//        }
//        return true;
//    }

    private static final String nl = System.getProperty("line.separator");
    private String toGraph(String head, Node t) {
        if (t == nil) return "";
        String graph = "";
        if (bottom(t))
            graph += head + t.ks() + nl;
        else {
            int i = t.ns().size();
            graph += toGraph(head + "    ", t.ns().get(--i));
            graph += head + "∧" + nl;
            do {
                graph += head + t.ks().get(--i) + nl;
                if (i == 0) graph += head + "∨" + nl;
                graph += toGraph(head + "    ", t.ns().get(i));
            } while (i > 0);
        }
        return graph;
    }

//    // 部分木 t の高さを返す
//    private int height(Node t) {
//        if (t == nil) return 0;
//        if (bottom(t)) return 1;
//        int mx = height(t.ns().get(0));
//        for (int i = 1; i < t.ns().size(); i++) {
//            int h = height(t.ns().get(i));
//            if (h > mx) mx = h;
//        }
//        return 1 + mx;
//    }
//
//    private boolean balanced(Node t) {
//        if (t == nil) return true;
//        if (bottom(t)) return true;
//        if (!balanced(t.ns().get(0))) return false;
//        int h = height(t.ns().get(0));
//        for (int i = 1; i < t.ns().size(); i++) {
//            if (!balanced(t.ns().get(i))) return false;
//            if (h != height(t.ns().get(i))) return false;
//        }
//        return true;
//    }
//
//    private boolean mstValid(Node t) {
//        if (t == nil) return true;
//        if (bottom(t)) {
//            for (int i = 1; i < t.ks().size(); i++) {
//                Key key1 = t.ks().get(i - 1);
//                Key key2 = t.ks().get(i);
//                if (!(key1.compareTo(key2) < 0)) return false;
//            }
//            return true;
//        }
//        Key key = t.ks().get(0);
//        if (!small(key, t.ns().get(0))) return false;
//        if (!mstValid(t.ns().get(0))) return false;
//        int i;
//        for (i = 1; i < t.ks().size(); i++) {
//            Key key1 = t.ks().get(i - 1);
//            Key key2 = t.ks().get(i);
//            if (!(key1.compareTo(key2) < 0)) return false;
//            if (!between(key1, key2, t.ns().get(i))) return false;
//            if (!mstValid(t.ns().get(i))) return false;
//        }
//        key = t.ks().get(i - 1);
//        if (!large(key, t.ns().get(i))) return false;
//        if (!mstValid(t.ns().get(i))) return false;
//        return true;
//    }
//
//    private boolean small(Key key, Node t) {
//        if (t == nil) return true;
//        if (bottom(t)) {
//            for (int i = 0; i < t.ks().size(); i++)
//                if (!(key.compareTo(t.ks().get(i)) > 0)) return false;
//            return true;
//        }
//        int i;
//        for (i = 0; i < t.ks().size(); i++) {
//            if (!small(key, t.ns().get(i))) return false;
//            if (!(key.compareTo(t.ks().get(i)) > 0)) return false;
//        }
//        if (!small(key, t.ns().get(i))) return false;
//        return true;
//    }
//
//    private boolean between(Key key1, Key key2, Node t) {
//        if (t == nil) return true;
//        if (bottom(t)) {
//            for (int i = 0; i < t.ks().size(); i++) {
//                if (!(key1.compareTo(t.ks().get(i)) <= 0)) return false;
//                if (!(key2.compareTo(t.ks().get(i)) >  0)) return false;
//            }
//            return true;
//        }
//        int i;
//        for (i = 0; i < t.ks().size(); i++) {
//            if (!between(key1, key2, t.ns().get(i))) return false;
//            if (!(key1.compareTo(t.ks().get(i)) <= 0)) return false;
//            if (!(key2.compareTo(t.ks().get(i)) >  0)) return false;
//        }
//        if (!between(key1, key2, t.ns().get(i))) return false;
//        return true;
//    }
//
//    private boolean large(Key key, Node t) {
//        if (t == nil) return true;
//        if (bottom(t)) {
//            for (int i = 0; i < t.ks().size(); i++)
//                if (!(key.compareTo(t.ks().get(i)) <= 0)) return false;
//            return true;
//        }
//        int i;
//        for (i = 0; i < t.ks().size(); i++) {
//            if (!large(key, t.ns().get(i))) return false;
//            if (!(key.compareTo(t.ks().get(i)) <= 0)) return false;
//        }
//        if (!large(key, t.ns().get(i))) return false;
//        return true;
//    }
//
//    private boolean denseHalf(Node t) {
//        if (t == nil) return true;
//        if (bottom(t)) {
//            final int n = t.ks().size();
//            if (n < hm-1 || m-1 < n) return false;
//        }
//        else {
//            final int n = t.ns().size();
//            if (n < hm || m < n) return false;
//            for (int i = 0; i < n; i++)
//                if (!denseHalf(t.ns().get(i))) return false;
//        }
//        return true;
//    }

}
