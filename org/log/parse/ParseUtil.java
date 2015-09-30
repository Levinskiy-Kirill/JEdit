package org.log.parse;

import org.codehaus.jackson.map.ObjectMapper;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.Selection;
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
	private static int iterator = -2;
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
		ArrayList<LogItem> sameItemType = new ArrayList<LogItem>();
		BufferedReader br = null;
		try {
			br = Files.newBufferedReader(Paths.get(filename), Charset.defaultCharset());
			LogItem item = readOneObject(br);
			
			while(item != null) {
				LogEventTypes type = item.getType();
				
				switch(type) {
					case CHARACTER_KEY :
						while(item.getType() == LogEventTypes.CHARACTER_KEY) {
							sameItemType.add(item);
							item = readOneObject(br);
						}
						allItems.add(sameItemType);
						sameItemType = new ArrayList<LogItem>();
						break;
						
					case SERVICE_KEY :
						while(item.getType() == LogEventTypes.SERVICE_KEY) {
							sameItemType.add(item);
							item = readOneObject(br);
						}
						allItems.add(sameItemType);
						sameItemType = new ArrayList<LogItem>();
						break;
						
					case SELECTION :
						LogItem lastSelection = null;
						while(item.getType() == LogEventTypes.SELECTION) {
							lastSelection = item;
							item = readOneObject(br);
						}
						sameItemType.add(lastSelection);
						allItems.add(sameItemType);
						sameItemType = new ArrayList<LogItem>();
						break;

					default :
						sameItemType.add(item);
						allItems.add(sameItemType);
						sameItemType = new ArrayList<LogItem>();
						item = readOneObject(br);
				}
			}
			
			/*while ((s = br.readLine()) != null && (!"".equals(s))) {
				previous = current;
				final LogItem item = mapper.readValue(s.getBytes(), LogItem.class);
				current = getLogItem(s, item.getType(), mapper);
				if(previous == null) {
					sameItemType.add(current);
					continue;
				}
				
				if(current.getType() != LogEventTypes.CHARACTER_KEY && previous.getType() == LogEventTypes.CHARACTER_KEY) {
					sameItemType.add(previous);
				} else if(current.getType() != LogEventTypes.SERVICE_KEY && previous.getType() == LogEventTypes.SERVICE_KEY) {
					sameItemType.add(previous);
				} else if(current.getType() != LogEventTypes.SELECTION && previous.getType() == LogEventTypes.SELECTION) {
					sameItemType.add(previous);
				} else if(current.getType() != LogEventTypes.SELECTION && previous.getType() == LogEventTypes.SELECTION) {
					sameItemType.add(previous);
					allItems.add(sameItemType);
					sameItemType = new ArrayList<LogItem>();
					sameItemType.add(current);
					continue;
				}

				allItems.add(sameItemType);
				sameItemType = new ArrayList<LogItem>();
				//sameItemType.add(current);

					
				
				if(current.getType() == previous.getType()) {
					if(current.getType() == LogEventTypes.SELECTION
						||current.getType() == LogEventTypes.SAVE_ACTION) {
						continue;
					}
					sameItemType.add(current);
				}
				else {
					if(previous.getType() == LogEventTypes.SELECTION
						|| previous.getType() == LogEventTypes.SAVE_ACTION) {
						sameItemType.add(previous);
					}
					allItems.add(sameItemType);
					sameItemType = new ArrayList<LogItem>();
					sameItemType.add(current);
				}
			}*/
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
	
	private static LogItem readOneObject(BufferedReader br) {
        LogItem item = null;
        try {
            String s;
            if((s = br.readLine()) != null) {
                item = mapper.readValue(s.getBytes(), LogItem.class);
                item = getLogItem(s, item.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return item;
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

	private static LogItem getLogItem(final String source, final LogEventTypes type) throws Exception {
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
				textArea.setCaretPosition(selection.getEnd());
				textArea.setSelection(selection.createSelection());				
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
	
	public static void previousAction() {
		if(iterator == -1)
			iterator = allItems.size() - 1;
		else if(iterator <= 0) {
			log.info("Start file");
			return;
		}
		
		--iterator;
		deleteAction();
	}
	
	private static void deleteAction() {
		ArrayList<LogItem> sameItems = allItems.get(iterator);
		LogEventTypes type = sameItems.get(0).getType();
		
		switch(type) {
			case SERVICE_KEY :
				deleteServiceKey(sameItems);
				break;
				
			case CHARACTER_KEY :
				deleteCharKey(sameItems);
				break;
				
			case SELECTION :
				LogSelection selection = (LogSelection)sameItems.get(sameItems.size() - 1);
				textArea.setCaretPosition(selection.getEnd());
				textArea.setCaretPosition(selection.getStart());				
				break;
				
			case CUT_ACTION :			
				deleteCutAction(sameItems);			
				break;
				
			case PASTE_ACTION :
				LogPaste paste = (LogPaste) sameItems.get(0);
				LogItem previousItem;
				ArrayList<LogItem> previousSameItems;
				if(iterator != 0) {
					previousSameItems = allItems.get(iterator - 1);
					previousItem = previousSameItems.get(previousSameItems.size() - 1);
					deletePasteAction(paste, previousItem);
				} else
					deletePasteAction(paste, null);
				
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
	
	/*private static boolean isTypeSetting(LogItem item) {
		if(item.getType().equals(arg0))
	}*/
	
	private static void deleteCutAction(ArrayList<LogItem> sameItems) {
		LogCut cut = (LogCut)sameItems.get(0);
		int startSelection = cut.getStart();
		int endSelection = startSelection + cut.getText().length();
		
		buffer.insert(startSelection, cut.getText());
		textArea.setSelection(new Selection.Range(startSelection, endSelection));
		JOptionPane.showMessageDialog(textArea, cut.getText(), "Cut action", JOptionPane.INFORMATION_MESSAGE);
	}
	
	private static void deletePasteAction(LogPaste item, LogItem previousItem) {
		buffer.remove(item.getPosition(), item.getText().length());
		
		if(previousItem != null && previousItem.getType().equals(LogEventTypes.SELECTION)) {
			int start = ((LogSelection)previousItem).getStart();
			int end = ((LogSelection)previousItem).getEnd();
			String text = ((LogSelection)previousItem).getSelectedText();
			buffer.insert(start, text);
			textArea.setSelection(new Selection.Range(start, end));
		}
	}
	
	private static void deleteCharKey(ArrayList<LogItem> sameItems) {
		int length = sameItems.size();
		int start = ((LogCharacterKey)sameItems.get(0)).getPosition();
		buffer.remove(start, length);
		if (buffer.getLength() < start)
			textArea.setCaretPosition(buffer.getLength());
		else
			textArea.setCaretPosition(start);
	}
	
	private static void deleteServiceKey(ArrayList<LogItem> sameItems) {
		int size = sameItems.size();
		for(int i = size-1; i >= 0; i--) {
			LogServiceKey servItem = (LogServiceKey)sameItems.get(i);
			if(isDeletedKey(servItem.getKeyCode())) {
				int start = servItem.getDeletedCharPosition();
				String deletedText = servItem.getDeletedText();
				int end = start + deletedText.length();
				buffer.insert(start, deletedText);
				if(deletedText.length() > 1)
					textArea.setSelection(new Selection.Range(start, end));
			} else {
				if (buffer.getLength() < servItem.getPosition())
					textArea.setCaretPosition(buffer.getLength());
				else
					textArea.setCaretPosition(servItem.getPosition());
			}
		}
	}
}
