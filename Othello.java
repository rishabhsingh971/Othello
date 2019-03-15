import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

public class Othello implements ActionListener {
	private JFrame frame;
	private player pl1, pl2, curr, oppo;
	private Color boardBG = Color.GREEN;
	private final int N = 8; // Board size

	/* value by which pos has to be updated to traverse in that direction */
	private final int RT = 1, LT = -1, UP = -1 * N, DN = N;
	/*
	 * other directions like Up-Left and Down-Right will be obtained from these 4
	 * directions (i.e sum of the 2 directions)
	 */

	private boolean p1Turn; /* Turn */
	private JButton[] btns = new JButton[N * N]; /* Board */
	private boolean notationShown, recordShown;/* whats shown on board */
	private boolean undoDone;
	/*
	 * Map of potential move's button and list of pos of opponent buttons it will
	 * conquer
	 */
	private HashMap<JButton, ArrayList<Integer>> options;

	/* Map to store if given pos is which boundary */
	private HashMap<Integer, Integer> boundaries;

	/* Records moves played till now */
	private ArrayList<JButton> record;
	private ArrayList<Integer> undoMoves;
	private JLabel stats;

	/* Status of Game */
	private enum status {
		complete, incomplete, noMove
	};

	/*
	 * comprises of no. of tiles, color of tile & color of hint tile for the player
	 */
	private class player {
		int count;
		Color col, hintCol, bestMoveCol;

		public player(int count, Color col, Color hintCol, Color bestMoveCol) {
			this.count = count;
			this.col = col;
			this.hintCol = hintCol;
			this.bestMoveCol = bestMoveCol;
		}
	}

	/* Constructor */
	public Othello() {
		setBoundaries();/* initializing boundary array */
		setPlayers(); /* setting colors to player acc. to turn */
		init();/* initializing window */
	}

	/* Initializing Function */
	private void init() {
		frame = new JFrame("Othello");
		frame.setLayout(new FlowLayout());
		frame.setSize(600, 600); /* Size of Window */
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);// });

		notationShown = false;
		recordShown = false;
		record = new ArrayList<>();

		JMenuBar menuBar = new JMenuBar();
		JMenu optionMenu = new JMenu("Options");

		JMenuItem restartOption = new JMenuItem("Restart");
		restartOption.setAccelerator(KeyStroke.getKeyStroke('R', KeyEvent.CTRL_DOWN_MASK));
		restartOption.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				restart();
			}
		});

		JMenuItem hintOption = new JMenuItem("Hint");
		hintOption.setAccelerator(KeyStroke.getKeyStroke('H'));
		hintOption.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				showBestMoves();
			}
		});

		JMenuItem notationOption = new JMenuItem("Show/Hide Notation");
		notationOption.setAccelerator(KeyStroke.getKeyStroke('N'));
		notationOption.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (recordShown) {
					toggleRecords();
				}
				toggleNotations();
			}
		});

		JMenuItem recordOption = new JMenuItem("Show/Hide Record");
		recordOption.setAccelerator(KeyStroke.getKeyStroke('R'));
		recordOption.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (notationShown) {
					toggleNotations();
				}
				toggleRecords();
			}
		});

		JMenuItem undoOption = new JMenuItem("Undo");
		undoOption.setAccelerator(KeyStroke.getKeyStroke('Z', KeyEvent.CTRL_DOWN_MASK));
		undoOption.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!undoDone && record.size() >= 1) {
					undo();
				}
			}
		});

		optionMenu.add(restartOption);
		optionMenu.addSeparator();
		optionMenu.add(hintOption);
		optionMenu.addSeparator();
		optionMenu.add(notationOption);
		optionMenu.add(recordOption);
		optionMenu.addSeparator();
		optionMenu.add(undoOption);

		menuBar.add(optionMenu);

		GridLayout glayout = new GridLayout(N, N);/* layout */
		JPanel btnPanel = new JPanel(glayout);
		/* adding buttons in window */
		for (int pos = 0; pos < N * N; ++pos) {
			btns[pos] = new JButton();/* Init */
			btns[pos].setEnabled(false);/* Default Disabled */
			btns[pos].addActionListener(this);
			btnPanel.add(btns[pos]);

			/* Adding starting buttons */
			int p2StartPos1 = (N / 2 - 1) * (N + 1), p2StartPos2 = (N / 2) * (N + 1);
			if (pos == p2StartPos1 || pos == p2StartPos2) {
				btns[pos].setBackground(pl2.col);
				continue;
			} else if (pos == p2StartPos1 + 1 || pos == p2StartPos2 - 1) {
				btns[pos].setBackground(pl1.col);
				continue;
			}
			btns[pos].setBackground(boardBG);// Non playable area

		}
		// Add Hints for player
		addHints();

		JPanel statusBar = new JPanel();
		stats = new JLabel();
		stats.setText("P1-" + pl1.count + "   P2-" + pl2.count);
		stats.setBorder(BorderFactory.createLoweredBevelBorder());
		statusBar.add(stats);

		Container contentPane = frame.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(btnPanel);
		contentPane.add(statusBar, BorderLayout.SOUTH);
		frame.setJMenuBar(menuBar);

		frame.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JButton bPressed = (JButton) e.getSource();
		bPressed.setEnabled(false);
		/* no need of below if coz only hint buttons are enabled */
		// if (bPressed.getBackground() == curr.hintCol)
		{
			Turn(bPressed);
			/* remove current player hints */
			undoMoves = new ArrayList<>(options.get(bPressed));
			remHints();
			/* Change player Designation Curr & Oppo */
			stats.setText("P1 : " + pl1.count + " P2: " + oppo.count);
			changePlayers();
			/* Adding hints */
			addHints();
		}
		if (recordShown) {
			bPressed.setText(record.size() + "");
		}
		checkStatus();
	}

	private void addHints() {
		options = new HashMap<>();

		// System.out.print((p1Turn ? "P1" : "P2") + "Moves : ");
		for (int i = 0; i < btns.length; ++i) {
			if (btns[i].getBackground() == curr.col) {
				paintHint(i);
			}
		}
		// System.out.println();
		// System.out.println("****************************");
	}

	private void Turn(JButton bPressed) {
		record.add(bPressed);
		bPressed.setBackground(curr.col);
		curr.count++;
		if (options.containsKey(bPressed)) {
			for (int i : options.get(bPressed)) {
				btns[i].setBackground(curr.col);
				curr.count++;
				oppo.count--;
			}
		}
		undoDone = false;
	}

	private void remHints() {
		for (int i = 0; i < btns.length; ++i) {
			Color btnBgCol = btns[i].getBackground();
			if (btnBgCol == curr.hintCol || btnBgCol == curr.bestMoveCol) {
				btns[i].setBackground(boardBG);
				btns[i].setEnabled(false);
			}
		}
	}

	private void paintHint(int pos) {
		/* Check Right */
		check(pos, RT);
		/* Check Left */
		check(pos, LT);
		/* Check Up */
		check(pos, UP);
		/* Check Down */
		check(pos, DN);
		/* Check Down Left */
		check(pos, DN + LT);
		/* Check Down Right */
		check(pos, DN + RT);
		/* Check Up Right */
		check(pos, UP + RT);
		/* Check Up Left */
		check(pos, UP + LT);

	}

	/*
	 * Checks for potential moves and store the potential tile and pos of tiles
	 * that'll be conquered if that tile is chosen
	 */
	private void check(int init, int dirUpd) {
		/* if init pos is not boundary for given direction */
		if (!isBoundforCurrDirection(init, dirUpd)) {
			init += dirUpd;
			int i = init;
			ArrayList<Integer> tempAL = new ArrayList<>();
			while (!isBoundforCurrDirection(i, dirUpd) && (btns[i].getBackground() == oppo.col)) {
				tempAL.add(i);
				i += dirUpd;
			}
			if (i < N * N && i >= 0 && (dirUpd > 0 ? i > init : i < init) && (btns[i].getBackground() == boardBG
					|| btns[i].getBackground() == curr.hintCol || btns[i].getBackground() == curr.bestMoveCol)) {
				btns[i].setBackground(curr.hintCol);
				btns[i].setEnabled(true);
				if (options.containsKey(btns[i])) {
					tempAL.addAll(options.get(btns[i]));
					options.remove(btns[i]);
				}
				// System.out.print(" [ " + (init - dirUpd) + "-" + tempAL + "-"
				// + i + " ], ");
				options.put(btns[i], tempAL);
			}
		}
	}

	private boolean isBoundforCurrDirection(int i, int dirUpd) {
		if (i >= N * N || i < 0) {
			return true;
		}
		if (boundaries.containsKey(i)) {
			int iBound = boundaries.get(i);
			if ((iBound == dirUpd) || (iBound == UP + LT && (dirUpd == UP || dirUpd == LT))
					|| (iBound == UP + RT && (dirUpd == UP || dirUpd == RT))
					|| (iBound == DN + LT && (dirUpd == DN || dirUpd == LT))
					|| (iBound == DN + RT && (dirUpd == DN || dirUpd == RT))
					|| (dirUpd == UP + LT && (iBound == UP || iBound == LT))
					|| (dirUpd == UP + RT && (iBound == UP || iBound == RT))
					|| (dirUpd == DN + LT && (iBound == DN || iBound == LT))
					|| (dirUpd == DN + RT && (iBound == DN || iBound == RT))) {
				return true;
			}
		}
		return false;
	}

	private void checkStatus() {
		status stat = getStatus();

		if (stat == status.noMove) {
			/* if there is no Move for current player */
			JOptionPane.showMessageDialog(frame, "No valid Move for P" + (p1Turn ? "1" : "2"));
			changePlayers();
			addHints();
			/* if opponent also has no move */
			if (options.isEmpty()) {
				stat = status.complete;
			}
		}
		/*
		 * No "else if" as change in noMove above can lead to completion of game
		 */
		if (stat == status.complete) {
			if (pl1.count > pl2.count) {
				JOptionPane.showMessageDialog(null, "P1 Won");
			} else if (pl2.count > pl1.count) {
				JOptionPane.showMessageDialog(frame, "P2 Won");
			} else {
				JOptionPane.showMessageDialog(frame, "Draw");
			}
			restart();
		}
		/* do nothing in any other case */

	}

	private status getStatus() {
		// System.out.println("Count : " + pl1.count + " " + pl2.count);
		if (pl1.count + pl2.count == N * N) {
			return status.complete;
		}
		if (options.size() > 0) {
			return status.incomplete;
		}
		if (options.size() == 0) {
			return status.noMove;
		}
		return status.complete;
	}

	private void toggleRecords() {
		if (recordShown) {
			for (JButton btn : record) {
				btn.setText("");
			}
		} else {
			int moveNum = 1;
			for (JButton btn : record) {
				btn.setText(moveNum + "");
				++moveNum;
			}
		}
		recordShown = !recordShown;

	}

	private void toggleNotations() {
		if (notationShown) {
			for (JButton btn : btns) {
				btn.setText("");
			}
		} else {
			int pos = 0;
			for (JButton btn : btns) {
				if (btn.getText() == "") {
					char alpha = (char) ('A' + (pos % N));
					int num = pos / N + 1;
					btn.setText(alpha + "" + num);
					++pos;

				}

			}
		}
		notationShown = !notationShown;
	}

	private void showBestMoves() {
		int maxSizeEntry = 0;
		/* contains best moves/options */
		ArrayList<JButton> bestOptions = new ArrayList<>();
		for (Entry<JButton, ArrayList<Integer>> entry : options.entrySet()) {
			if (entry.getValue().size() > maxSizeEntry) {
				maxSizeEntry = entry.getValue().size();
			}
		}
		for (Entry<JButton, ArrayList<Integer>> entry : options.entrySet()) {
			if (entry.getValue().size() == maxSizeEntry) {
				bestOptions.add(entry.getKey());
			}
		}

		for (JButton btn : bestOptions) {
			btn.setBackground(curr.bestMoveCol);
			;
		}

	}

	private void restart() {
		int choice = JOptionPane.showConfirmDialog(null, "Restart?");
		if (choice == JOptionPane.YES_OPTION) {
			frame.dispose();
			setPlayers();
			init();
		}

	}

	private void setBoundaries() {
		boundaries = new HashMap<>();
		/* Up Boundary */
		for (int i = 0; i < N; ++i) {
			boundaries.put(i, UP);
		}

		/* Down */
		for (int i = N * (N - 1); i < N * N; ++i) {
			boundaries.put(i, DN);
		}

		/* Right , Upper Right & Down Right) */
		for (int i = N - 1; i < N * N; i += N) {
			if (!boundaries.containsKey(i)) {
				boundaries.put(i, RT);
			} else // for UPRT or DNRT
			{
				boundaries.put(i, boundaries.get(i) + RT);
			}
		}

		/* Left, Upper Left & Down Left */
		for (int i = 0; i < N * N; i += N) {
			if (!boundaries.containsKey(i)) {
				boundaries.put(i, LT);
			} else {
				boundaries.put(i, boundaries.get(i) + LT);
			}
		}
	}

	private void setPlayers() {
		pl1 = new player(2, Color.BLACK, Color.RED, Color.ORANGE);
		pl2 = new player(2, Color.WHITE, Color.GRAY, Color.CYAN);
		p1Turn = true;
		curr = pl1;
		oppo = pl2;
	}

	private void changePlayers() {
		p1Turn = !p1Turn;
		curr = p1Turn ? pl1 : pl2;
		oppo = p1Turn ? pl2 : pl1;
	}

	private void undo() {
		undoDone = true;
		remHints();
		JButton lastMove = record.get(record.size() - 1);
		record.remove(record.size() - 1);
		if (recordShown) {
			lastMove.setText("");
		}
		lastMove.setBackground(boardBG);
		System.out.println(undoMoves);
		for (int i : undoMoves) {
			btns[i].setBackground(curr.col);
		}
		changePlayers();
		addHints();
	}

}
