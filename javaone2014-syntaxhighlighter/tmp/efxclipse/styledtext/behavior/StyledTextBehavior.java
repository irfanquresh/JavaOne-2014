/*******************************************************************************
* Copyright (c) 2013 BestSolution.at and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* 	Tom Schindl<tom.schindl@bestsolution.at> - initial API and implementation
*******************************************************************************/
package at.bestsolution.efxclipse.styledtext.behavior;

import java.util.Set;

import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import at.bestsolution.efxclipse.styledtext.ActionEvent;
import at.bestsolution.efxclipse.styledtext.StyledTextArea;
import at.bestsolution.efxclipse.styledtext.ActionEvent.ActionType;
import at.bestsolution.efxclipse.styledtext.VerifyEvent;
import at.bestsolution.efxclipse.styledtext.skin.StyledTextSkin.LineCell;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.text.HitInfo;

public class StyledTextBehavior extends BehaviorBase<StyledTextArea> {
	public StyledTextBehavior(StyledTextArea styledText) {
		super(styledText);
	}

	@Override
	protected void callActionForEvent(KeyEvent arg0) {
		if( arg0.getEventType() == KeyEvent.KEY_PRESSED ) {
			keyPressed(arg0, getControl().getContent().getLineAtOffset(getControl().getCaretOffset()));
		}
		super.callActionForEvent(arg0);
	}
	
	@Override
	protected void callAction(String arg0) {
		super.callAction(arg0);
	}
	
	@Override
	public void mousePressed(MouseEvent arg0) {
		super.mousePressed(arg0);
		getControl().requestFocus();
	}
	
	public boolean invokeAction(ActionType action) {
		ActionEvent evt = new ActionEvent(getControl(), getControl(), action);
		Event.fireEvent(getControl(), evt);
		return evt.isConsumed();
	}
	
	private void keyPressed(KeyEvent event, int currentRowIndex) {
		VerifyEvent evt = new VerifyEvent(getControl(), getControl(), event);
		Event.fireEvent(getControl(), evt);
		
		if( evt.isConsumed() ) {
			event.consume();
			return;
		}
		
		final int offset = getControl().getCaretOffset();
		
		switch (event.getCode()) {
		case SHIFT:
		case ALT:
		case CONTROL:
			break;
		case LEFT:
		{
			if( event.isAltDown() ) {
				invokeAction(ActionType.WORD_PREVIOUS);
			} else {
				if( offset == 0 ) {
					break;
				}
				int newOffset = offset-1;
				int currentLine = getControl().getContent().getLineAtOffset(offset);
				int newLine = getControl().getContent().getLineAtOffset(newOffset);
				getControl().setCaretOffset(newOffset);				
			}
			break;
		}
		case RIGHT:
		{
			if( event.isAltDown() ) {
				invokeAction(ActionType.WORD_NEXT);
			} else if(event.isMetaDown())  {
				int currentLine = getControl().getContent().getLineAtOffset(offset);
				int lineOffset = getControl().getContent().getOffsetAtLine(currentLine);
				String lineContent = getControl().getContent().getLine(currentLine);
				
				getControl().setCaretOffset(lineOffset + lineContent.length());
			} else {
				if( offset+1 > getControl().getContent().getCharCount() ) {
					break;
				}
				int newOffset = offset+1;
				int currentLine = getControl().getContent().getLineAtOffset(offset);
				int newLine = getControl().getContent().getLineAtOffset(newOffset);
				getControl().setCaretOffset(newOffset);				
			}
			break;
		}
		case UP:
		{
			int rowIndex = currentRowIndex;
			
			if( rowIndex == 0 ) {
				break;
			}
			
			int colIdx = offset - getControl().getContent().getOffsetAtLine(rowIndex);
			rowIndex -= 1;
			
			int lineOffset = getControl().getContent().getOffsetAtLine(rowIndex);
			int newCaretPosition = lineOffset + colIdx;
			int maxPosition      = lineOffset + getControl().getContent().getLine(rowIndex).length();
			
			getControl().setCaretOffset(Math.min(newCaretPosition, maxPosition));
			break;
		}
		case DOWN:
		{
			int rowIndex = currentRowIndex;
			if( rowIndex+1 == getControl().getContent().getLineCount() ) {
				break;
			}
			
			int colIdx = offset - getControl().getContent().getOffsetAtLine(rowIndex);
			rowIndex += 1;
			
			int lineOffset = getControl().getContent().getOffsetAtLine(rowIndex);
			int newCaretPosition = lineOffset + colIdx;
			int maxPosition      = lineOffset + getControl().getContent().getLine(rowIndex).length();
			
			getControl().setCaretOffset(Math.min(newCaretPosition, maxPosition));
			break;
		}
		case ENTER:
			int line = getControl().getContent().getLineAtOffset(getControl().getCaretOffset());
			String lineContent = getControl().getContent().getLine(line);
			
			//FIXME Temp hack
			char[] chars = lineContent.toCharArray();
			String prefix = "";
			for( int i = 0; i < chars.length; i++ ) {
				if( chars[i] == ' ' ) {
					prefix += " ";	
				} else {
					break;
				}
			}
			
			getControl().getContent().replaceTextRange(getControl().getCaretOffset(), 0, event.getText()+prefix);
//			listView.getSelectionModel().select(listView.getSelectionModel().getSelectedIndex()+1);
			getControl().setCaretOffset(offset+1+prefix.length());
			break;
		case DELETE:
			if( event.isMetaDown() ) {
				invokeAction(ActionType.DELETE_WORD_NEXT);
			} else {
				getControl().getContent().replaceTextRange(getControl().getCaretOffset(), 1, "");
				getControl().setCaretOffset(offset);				
			}
			break;
		case BACK_SPACE:
			if( event.isMetaDown() ) {
				invokeAction(ActionType.DELETE_WORD_PREVIOUS);
			} else {
				getControl().getContent().replaceTextRange(getControl().getCaretOffset()-1, 1, "");
				getControl().setCaretOffset(offset-1);	
			}
			break;
		case TAB:
			event.consume();
			if( event.isShiftDown() ) {
				//TODO Remove first 4 white space chars???
				break;
			} else {
				//FIXME Need to should fix this but it currently completely break cursor positioning
				getControl().getContent().replaceTextRange(getControl().getCaretOffset(), 0, "    ");
				getControl().setCaretOffset(offset+4);
				break;
			}
		default:
			if( event.isMetaDown() || event.isControlDown() ) {
				// exclude meta keys
			} else {
				
				if( event.getText().length() > 0 ) {
					getControl().getContent().replaceTextRange(getControl().getCaretOffset(), 0, event.getText());
					getControl().setCaretOffset(offset+1);	
				}
			}
			
			break;
		}
		
//		Event.fireEvent(getControl(), event.copyFor(getControl(), getControl()));
	}
	
	public void mousePressed(MouseEvent event, Set<LineCell> visibleCells) {
		Point2D p = new Point2D(event.getX(), event.getY());
		for( LineCell cell : visibleCells ) {
			if( cell.getBoundsInParent().contains(p) ) {
				if( cell.getDomainElement() != null ) {
					// Calculate to cell relative
					p = p.subtract(cell.getLayoutX(), cell.getLayoutY());
					Region g = (Region) cell.getGraphic();
					p = p.subtract(g.getLayoutX(), g.getLayoutY());
					
					TextFlow flow = (TextFlow) g.getChildrenUnmodifiable().get(0);
					// Calculate to text flow
					p = p.subtract(flow.getLayoutX(), flow.getLayoutY());
					for( Node n : flow.getChildren() ) {
						Text text = (Text) n;
						if( text.getBoundsInParent().contains(p) ) {
							HitInfo info = text.impl_hitTestChar(new Point2D(p.getX()-text.getLayoutX(), 0 /* See RT-28485 text.getLayoutY()*/));
							if( info.getInsertionIndex() >= 0 ) {
//								System.err.println("Text: " + text.getText());
//								System.err.println("Text-Offset: " + text.getUserData());
//								System.err.println("INSERT INDEX: " + info.getInsertionIndex());
								int offset = ((Integer)text.getUserData()).intValue()+info.getInsertionIndex();
//								System.err.println("NEW OFFSET AT: " + offset);
								getControl().setCaretOffset(offset);
								return;
							}
						}
					}
					
					int offset = cell.getDomainElement().getLineOffset() + cell.getDomainElement().getLineLength();
					getControl().setCaretOffset(offset);
				}
				break;
			}
		}
		getControl().requestFocus();
		Event.fireEvent(getControl(), event.copyFor(getControl(), getControl()));
	}
}
