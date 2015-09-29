package org.log.parse;

import org.codehaus.jackson.map.ObjectMapper;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;
import org.log.LogCharacterKey;
import org.log.LogCloseFile;
import org.log.LogCompile;
import org.log.LogCopy;
import org.log.LogCut;
import org.log.LogEventTypes;
import org.log.LogItem;
import org.log.LogKey;
import org.log.LogOpenFile;
import org.log.LogPaste;
import org.log.LogRun;
import org.log.LogSaveFile;
import org.log.LogSelection;
import org.log.LogSelectionClear;
import org.log.LogServiceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class ParseUtil {
	private static final Logger log = LoggerFactory.getLogger(ParseUtil.class);
	private static ObjectMapper mapper = new ObjectMapper();
	
	private static ArrayList<ArrayList<LogItem>> allItems;
	//private static ArrayList<LogItem> currentGroupSameItems;
	private static int iterator = -1;
	private static JEditTextArea textArea;
	private static JEditBuffer buffer;
	//private static LogItem current;
	
	public static void parseLog(JEditTextArea area, JEditBuffer buf) {
		try {
			buffer = buf;
			textArea = area;
			if (openLogFile()) {
				//currentGroupSameItems = allItems.get(0);
				iterator = 0;
			}
			else
				log.info("Error open File");
		} catch (Exception ex) {
			Log.log(Log.ERROR, textArea, "Something went wrong", ex);
		}
		//ViewMain viewMain = new ViewMain(items, this);		
	}
	
	public static void parseFile(final String filename) throws Exception {
		allItems = new ArrayList<ArrayList<LogItem>>();
		BufferedReader br = null;
		try {
			br = Files.newBufferedReader(Paths.get(filename), Charset.defaultCharset());
			String s;
			ArrayList<LogItem> sameItemType = new ArrayList<LogItem>();
			LogItem current = null;
			LogItem previous = null;
			
			while ((s = br.readLine()) != null && (!"".equals(s))) {
				previous = current;
				final LogItem item = mapper.readValue(s.getBytes(), LogItem.class);
				current = getLogItem(s, item.getType(), mapper);
				if(previous == null) {
					sameItemType.add(current);
					continue;
				}
				
				/*if(current.getType().equals(LogEventTypes.CHARACTER_KEY) && previous.getType().equals(LogEventTypes.CHARACTER_KEY)) {
					sameItemType.add(current);
					continue;
				} else if(current.getType().equals(LogEventTypes.SERVICE_KEY) && previous.getType().equals(LogEventTypes.SERVICE_KEY)) {
					sameItemType.add(current);
					continue;
				} else if(!current.getType().equals(LogEventTypes.SELECTION) && previous.getType().equals(LogEventTypes.SELECTION)) {
					sameItemType.add(previous);
				}

				allItems.add(sameItemType);
				sameItemType = new ArrayList<LogItem>();
				sameItemType.add(current);*/

					
				
				if(current.getType() == previous.getType()) {
					if(current.getType().equals(LogEventTypes.SELECTION)
						||current.getType().equals(LogEventTypes.SAVE_ACTION)) {
						continue;
					}
					sameItemType.add(current);
				}
				else {
					if(previous.getType().equals(LogEventTypes.SELECTION)
						|| previous.getType().equals(LogEventTypes.SAVE_ACTION)) {
						sameItemType.add(previous);
					}
					allItems.add(sameItemType);
					sameItemType = new ArrayList<LogItem>();
					sameItemType.add(current);
				}
			}
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					System.out.println("Cannot close reader");
				}
			}
		}
	}
	
	public static void nextAction() {
		if(iterator == -1) {
			log.info("End of file");
			return;
		}
		
		log.info("IN PRESS NEXT ACTION AT PARSEUTIL. ITERATOR: " + iterator);

		displayAction();
		if(iterator >= allItems.size())
			iterator = -1;
		else
			++iterator;
	}

	private static LogItem getLogItem(final String source, final LogEventTypes type, final ObjectMapper mapper) throws Exception {
		switch (type) {
			case CHARACTER_KEY:
				return mapper.readValue(source.getBytes(), LogCharacterKey.class);
			case SAVE_ACTION:
				return mapper.readValue(source.getBytes(), LogSaveFile.class);
			case SELECTION:
				return mapper.readValue(source.getBytes(), LogSelection.class);
			case SELECTION_CLEAR:
				return mapper.readValue(source.getBytes(), LogSelectionClear.class);
			case SERVICE_KEY:
				return mapper.readValue(source.getBytes(), LogServiceKey.class);
			case OPEN_ACTION:
				return mapper.readValue(source.getBytes(), LogOpenFile.class);
			case CLOSE_ACTION:
				return mapper.readValue(source.getBytes(), LogCloseFile.class);
			case COMPILE_ACTION:
				return mapper.readValue(source.getBytes(), LogCompile.class);
			case RUN_ACTION:
				return mapper.readValue(source.getBytes(), LogRun.class);
			case PASTE_ACTION:
				return mapper.readValue(source.getBytes(), LogPaste.class);
			case COPY_ACTION:
				return mapper.readValue(source.getBytes(), LogCopy.class);
			case CUT_ACTION:
				return mapper.readValue(source.getBytes(), LogCut.class);
			default:
				throw new IllegalArgumentException("there is no such type");
		}
	}
	
	private static boolean openLogFile() throws Exception {
		JFileChooser chooser = new JFileChooser(Paths.get("logs").toFile());

		if (chooser.showOpenDialog(textArea) == JFileChooser.APPROVE_OPTION) {
			ParseUtil.parseFile(chooser.getSelectedFile().getAbsolutePath());
			
			log.info("All items: ");
			
			for(ArrayList<LogItem> sameItems : allItems) {
				for(LogItem item : sameItems) {
					try {
		                log.info(mapper.writeValueAsString(item));
		            } catch (Exception e) {
		                Log.log(Log.ERROR, null, "Cannot write copy action to json", e);
		            }
				}
				log.info("");
				log.info("");
				log.info("");
			}

			return true;
		} else {
			return false;
		}
	}
	
	private static void displayAction() {
		log.info("IN DISPLAY ACTION");
		ArrayList<LogItem> sameItems = allItems.get(iterator);
		LogEventTypes type = sameItems.get(0).getType();
		
		switch(type) {
			case SERVICE_KEY :
				pressServiceKey(sameItems);
				break;
				
			case CHARACTER_KEY :
				pressCharKey(sameItems);
				break;
				
			case SELECTION :
				LogSelection selection = (LogSelection)sameItems.get(sameItems.size() - 1);
				textArea.setSelection(selection.createSelection());
				textArea.setCaretPosition(selection.getEnd());
				break;
				
			case CUT_ACTION :			
				LogCut cut = (LogCut)sameItems.get(0);
				simulateCutAction(cut);
				JOptionPane.showMessageDialog(textArea, cut.getText(), "Cut action", JOptionPane.INFORMATION_MESSAGE);			
				break;
				
			case PASTE_ACTION :
				LogPaste paste = (LogPaste) sameItems.get(0);
				LogItem previousItem;
				ArrayList<LogItem> previousSameItems;
				if(iterator != 0) {
					previousSameItems = allItems.get(iterator - 1);
					previousItem = previousSameItems.get(previousSameItems.size() - 1);
					simulatePasteAction(paste, previousItem);
				} else
					simulatePasteAction(paste, null);
				
				break;
				
			case COPY_ACTION :
				LogCopy copy = (LogCopy)sameItems.get(0);
				JOptionPane.showMessageDialog(textArea, copy.getText(), "Copy action", JOptionPane.INFORMATION_MESSAGE);
				break;
				
			case COMPILE_ACTION :
				LogCompile compile = (LogCompile)sameItems.get(0);
				JOptionPane.showMessageDialog(textArea, compile.getText(), "Compile action", JOptionPane.INFORMATION_MESSAGE);
				break;
				
			case RUN_ACTION :
				LogRun run = (LogRun)sameItems.get(0);
				JOptionPane.showMessageDialog(textArea, run.getText(), "Run action", JOptionPane.INFORMATION_MESSAGE);
				break;
				
			case SAVE_ACTION :
				LogSaveFile save = (LogSaveFile)sameItems.get(0);
				JOptionPane.showMessageDialog(textArea, save.getPath(), "Save action", JOptionPane.INFORMATION_MESSAGE);
				break;
		}
	}
	
	private static void pressServiceKey(ArrayList<LogItem> serviceItems) {
		log.info("IN PRESS SERVICE KEY");
		for(LogItem item : serviceItems) {
			try {
                log.info(mapper.writeValueAsString(item));
            } catch (Exception e) {
                Log.log(Log.ERROR, null, "Cannot write copy action to json", e);
            }
		}
		log.info("");
		log.info("");
		
		Robot robot = null;		
		try {
			robot = new Robot();
		} catch (AWTException e) {
			log.info("Error while creating robot for simulate pressing service key");
			return;
		}
		
		for(LogItem item : serviceItems) {
			LogServiceKey serviceItem = (LogServiceKey)item;
			textArea.setCaretPosition(getCaretForServiceKey(serviceItem));
			if(isDeletedKey(serviceItem.getKeyCode()))
				buffer.remove(serviceItem.getDeletedCharPosition(), serviceItem.getDeletedText().length());
			else {
				robot.keyPress(serviceItem.getKeyCode());
				robot.keyRelease(serviceItem.getKeyCode());
			}
		}
	}
	
	private static boolean isDeletedKey(final int keyCode) {
		return keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE;
	}
	
	private static void pressCharKey(ArrayList<LogItem> charItems) {	
		int counter = 0;
		for(LogItem item : charItems) {		
			LogCharacterKey charItem = (LogCharacterKey)item;
			buffer.insert(charItem.getPosition(), "" + charItem.getKeyChar());
			textArea.setCaretPosition(charItem.getPosition());
		}
	}
	
	private static boolean isShiftRequired(LogCharacterKey item) {
		return item.getMask() == KeyEvent.SHIFT_MASK;
	}
	
	private static int getCaretForServiceKey(LogServiceKey item)
	{
		/*if (item.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
			return item.getPosition() + 1;
		} else*/ 
			if (buffer.getLength() < item.getPosition()) {
			return buffer.getLength();
		} else {
			return item.getPosition();
		}
	}
	
	private static void simulateCutAction(LogCut item) {
		buffer.remove(item.getStart(), item.getText().length());
		textArea.setCaretPosition(item.getStart());
	}
	
	private static void simulatePasteAction(LogPaste item, LogItem previousItem) {
		if(previousItem != null && previousItem.getType().equals(LogEventTypes.SELECTION))
			buffer.remove(((LogSelection)previousItem).getStart(), ((LogSelection)previousItem).getSelectedText().length());

		buffer.insert(item.getPosition(), item.getText());
		textArea.setCaretPosition(item.getPosition() + item.getText().length());
	}
}
