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
package at.bestsolution.efxclipse.styledtext.skin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;
import javafx.util.Duration;
import at.bestsolution.efxclipse.styledtext.StyleRange;
import at.bestsolution.efxclipse.styledtext.StyledTextArea;
import at.bestsolution.efxclipse.styledtext.behavior.StyledTextBehavior;

import com.sun.javafx.scene.control.skin.BehaviorSkinBase;

@SuppressWarnings("restriction")
public class StyledTextSkin extends BehaviorSkinBase<StyledTextArea, StyledTextBehavior> {
	private ListView<Line> listView;
	
	private ObservableList<Line> lineList = FXCollections.observableArrayList();
	
	private Set<LineCell> visibleCells = new HashSet<>();

	private Font boldFont;
	
	private Font boldItalicFont;
	
	private Font italicFont;
	
	public StyledTextSkin(StyledTextArea styledText) {
		super(styledText, new StyledTextBehavior(styledText));
		
		listView = new ListView<>();
		listView.setFocusTraversable(false);
		listView.setCellFactory(new Callback<ListView<Line>, ListCell<Line>>() {
			
			@Override
			public ListCell<Line> call(ListView<Line> arg0) {
				return new LineCell();
			}
		});
		listView.setMinHeight(0);
		listView.setMinWidth(0);
		listView.setOnMousePressed(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				getBehavior().mousePressed(event, visibleCells);
				// The consuming does not help because it looks like the
				// selection change happens earlier => should be push a new ListViewBehavior?
				event.consume();
			}
		});
		
		recalculateItems();
		
		listView.setItems(lineList);
		getChildren().addAll(listView);
		
		styledText.caretOffsetProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				int lineIndex = getSkinnable().getContent().getLineAtOffset(newValue.intValue());
				Line lineObject = lineList.get(lineIndex);
				for( LineCell c : visibleCells ) {
					if( c.domainElement == lineObject ) {
						// Adjust the selection
						if( listView.getSelectionModel().getSelectedItem() != c.domainElement ) {
							listView.getSelectionModel().select(lineObject);
						}
						
						RegionImpl container = (RegionImpl)c.getGraphic();
						TextFlow flow = (TextFlow)container.getChildren().get(0);
						
						flow.requestLayout();
						
						break;
					}
				}
			}
		});
	}
	
	public double getLineHeight(int caretPosition) {
		int lineIndex = getSkinnable().getContent().getLineAtOffset(caretPosition);
		Line lineObject = lineList.get(lineIndex);
		
		for( LineCell c : visibleCells ) {
			if( c.domainElement == lineObject ) {
				return c.getHeight();
			}
		}
		return 0;
	}
	
	@SuppressWarnings("deprecation")
	public Point2D getCaretLocation(int caretPosition) {
		if( caretPosition < 0 ) {
			return null;
		}
		
		int lineIndex = getSkinnable().getContent().getLineAtOffset(caretPosition);
		Line lineObject = lineList.get(lineIndex);
		for( LineCell c : visibleCells ) {
			if( c.domainElement == lineObject ) {
				RegionImpl container = (RegionImpl)c.getGraphic();
				TextFlow flow = (TextFlow)container.getChildren().get(0);
				System.err.println("STARTING SCAN");
				Text textNode = null;
				int relativePos = 0;
				for( int i = flow.getChildren().size()-1; i >= 0; i-- ) {
					Node n = flow.getChildren().get(i);
//					System.err.println(((Text)n).getText() + " => " + n.getLayoutX());
					int offset = ((Integer) n.getUserData()).intValue();
					if( offset <= caretPosition ) {
						relativePos = caretPosition - offset;
						textNode = (Text) n;
						break;
					}
				}
				
				if( textNode != null ) {
					textNode.setImpl_caretPosition(relativePos);
					PathElement[] elements = textNode.getImpl_caretShape();
					double xShift = textNode.getLayoutX();
					System.err.println(textNode.getText() + " ====> " + xShift);
					for( PathElement e : elements ) {
						if( e instanceof MoveTo ) {
							xShift +=((MoveTo)e).getX();
						}
					}
					System.err.println("==> " + xShift);
					
					Point2D rv = new Point2D(xShift, c.getLayoutY());
					return rv;
//					final Path p = (Path)container.getChildren().get(1); 
//					
//					p.getElements().clear();
//					p.getElements().addAll(textNode.getImpl_caretShape());
//					
//					p.setLayoutX(textNode.getLayoutX());
//					p.setLayoutY(textNode.getBaselineOffset());
				}
				
				
//				RegionImpl container = (RegionImpl)c.getGraphic();
//				
//				final Path p = (Path)container.getChildren().get(1);
//				Point2D rv = new Point2D(p.getLayoutX(),container.getLayoutY());
//				System.err.println("CARE-LOC: " + rv);
//				return rv;
			}
		}
		
		return null;
	}
	
	@Override
	protected double computeMinHeight(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		return listView.minHeight(height);
	}
	
	@Override
	protected double computeMinWidth(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		return listView.minWidth(width);
	}
    
//    @Override
//	protected double computeMinHeight(double height) {
//		return listView.minHeight(height);
//	}
//	
//	@Override
//	protected double computeMinWidth(double width) {
//		return listView.minWidth(width);
//	}
	
	public void recalculateItems() {
		if( lineList.size() != getSkinnable().getContent().getLineCount() ) {
			if( lineList.size() > getSkinnable().getContent().getLineCount() ) {
				lineList.remove(getSkinnable().getContent().getLineCount(), lineList.size());
			} else {
				List<Line> tmp = new ArrayList<>(getSkinnable().getContent().getLineCount()-lineList.size());
				for( int i = lineList.size(); i < getSkinnable().getContent().getLineCount(); i++ ) {
					tmp.add(new Line());
				}
				lineList.addAll(tmp);
			}
		}
		
		redraw();
	}
	
	public void redraw() {
		for( LineCell l : visibleCells ) {
			l.update();
		}
	}
	
	Font getFontByStyle(int style) {
		switch (style) {
			case StyleRange.BOLD:
				if (boldFont != null) return boldFont;
				return boldFont = createFont(style);
			case StyleRange.ITALIC:
				if (italicFont != null) return italicFont;
				return italicFont = createFont(style);
			case StyleRange.BOLD | StyleRange.ITALIC:
				if (boldItalicFont != null) return boldItalicFont;
				return boldItalicFont = createFont(style);
			default:
				return getSkinnable().fontProperty().get();
		}
	}
	
	Font createFont(int style) {
		switch (style) {
		case StyleRange.BOLD:
		{
			Font f = Font.font(getSkinnable().getFont().getFamily(), FontWeight.BOLD, getSkinnable().getFont().getSize());
			return f;
		}
		case StyleRange.ITALIC:
		{
			Font f = Font.font(getSkinnable().getFont().getFamily(), FontPosture.ITALIC, getSkinnable().getFont().getSize());
			return f;
		}
		case StyleRange.BOLD | StyleRange.ITALIC:
		{
			Font f = Font.font(getSkinnable().getFont().getFamily(), FontWeight.BOLD, FontPosture.ITALIC, getSkinnable().getFont().getSize());
			return f;
		}	
		}
		return null;
	}
	
	public class LineCell extends ListCell<Line> {
		private Line domainElement;
		private BooleanBinding caretVisible;
		private BooleanProperty flashProperty;
		private Timeline flashTimeline;
		
		public LineCell() {
			flashProperty = new SimpleBooleanProperty(this,"flash",false);
			flashTimeline = new Timeline();
			flashTimeline.setCycleCount(Timeline.INDEFINITE);
			
			EventHandler<ActionEvent> startEvent = new EventHandler<ActionEvent>() {
				
				@Override
				public void handle(ActionEvent arg0) {
					flashProperty.set(true);
				}
			};
			
			EventHandler<ActionEvent> endEvent = new EventHandler<ActionEvent>() {
				
				@Override
				public void handle(ActionEvent arg0) {
					flashProperty.set(false);
				}
			};
			
			flashTimeline.getKeyFrames().addAll(new KeyFrame(Duration.ZERO, startEvent), new KeyFrame(Duration.millis(500), endEvent), new KeyFrame(Duration.millis(1000)));
			caretVisible = new BooleanBinding() {
				{
					bind(selectedProperty(), flashProperty);
				}
				@Override
				protected boolean computeValue() {
					return selectedProperty().get() && flashProperty.get();
				}
			};
			selectedProperty().addListener(new ChangeListener<Boolean>() {

				@Override
				public void changed(ObservableValue<? extends Boolean> arg0,
						Boolean arg1, Boolean arg2) {
					if( arg2.booleanValue() ) {
						flashTimeline.play();
					} else {
						flashTimeline.stop();
					}
				}
			});
		}
		
		public Line getDomainElement() {
			return domainElement;
		}
		
		public void update() {
			if( domainElement != null ) {
				updateItem(domainElement, false);	
			}
		}
		
		public void updateCaret() {
			//FIXME Could not pass on the Region?
			int caretPosition = getSkinnable().getCaretOffset();
			
			if( caretPosition < 0 ) {
				return;
			}
			
			int lineIndex = getSkinnable().getContent().getLineAtOffset(caretPosition);
			Line lineObject = lineList.get(lineIndex);
			for( LineCell c : visibleCells ) {
				if( c.domainElement == lineObject ) {
					RegionImpl container = (RegionImpl)c.getGraphic();
					TextFlow flow = (TextFlow)container.getChildren().get(0);
					
					
					Text textNode = null;
					int relativePos = 0;
					for( int i = flow.getChildren().size()-1; i >= 0; i-- ) {
						Node n = flow.getChildren().get(i);
						int offset = ((Integer) n.getUserData()).intValue();
						if( offset <= caretPosition ) {
							relativePos = caretPosition - offset;
							textNode = (Text) n;
							break;
						}
					}
					
					if( textNode != null ) {
						textNode.setImpl_caretPosition(relativePos);
						
						final Path p = (Path)container.getChildren().get(1); 
						
						p.getElements().clear();
						p.getElements().addAll(textNode.getImpl_caretShape());
						
						p.setLayoutX(textNode.getLayoutX());
						p.setLayoutY(textNode.getBaselineOffset());
					}
					
					break;
				}
			}
		}
		
		@Override
		protected void updateItem(Line arg0, boolean arg1) {
			if( ! arg1 ) {
				domainElement = arg0;
				visibleCells.add(this);
				
				RegionImpl stack = (RegionImpl) getGraphic();
				TextFlow flow;
				
				if( stack == null ) {
					flow = new TextFlow() {
						@Override
						protected void layoutChildren() {
							super.layoutChildren();
							updateCaret();
						}
					};
					Path caretPath = new Path();
					caretPath.setManaged(false);
					caretPath.setStrokeWidth(1);
			        caretPath.setFill((Color.BLACK));
			        caretPath.setStroke((Color.BLACK));
			        caretPath.visibleProperty().bind(caretVisible);
					stack = new RegionImpl(flow,caretPath);
					setGraphic(stack);
				} else {
					flow = (TextFlow) stack.getChildren().get(0);
				}
				
				List<Text> texts = new ArrayList<>();
				for( final Segment seg : arg0.getSegments() ) {
					final Text t = new Text(seg.text);
					t.setUserData(seg.style.start);
					if( seg.style.foreground != null ) {
						t.setFill(seg.style.foreground);
					}
					if( seg.style.font != null ) {
						t.setFont(seg.style.font);
					} else {
						t.setFont(getFontByStyle(seg.style.fontStyle));
					}
					
					if( seg.style.underline ) {
						System.err.println("=====================> UNDERLINEING");
					}
					
					texts.add(t);
				}
				
				if( texts.isEmpty() ) {
					Text t = new Text("");
					t.setUserData(arg0.getLineOffset());
					texts.add(t);
				}
				
				flow.getChildren().setAll(texts);
				stack.requestLayout();
			} else {
				setGraphic(null);
				domainElement = null;
				visibleCells.remove(this);
			}
			super.updateItem(arg0, arg1);
		}
	}
	
	static class RegionImpl extends Region {
		public RegionImpl(Node... nodes) {
			getChildren().addAll(nodes);
		}
		
		@Override
		public ObservableList<Node> getChildren() {
			// TODO Auto-generated method stub
			return super.getChildren();
		}
	}
	
	public class Line {
		public String getText() {
			return removeLineending(getSkinnable().getContent().getLine(lineList.indexOf(this)));
		}
		
		public int getLineOffset() {
			int idx = lineList.indexOf(this);
			return getSkinnable().getContent().getOffsetAtLine(idx);
		}
		
		public int getLineLength() {
			int idx = lineList.indexOf(this);
			String s = getSkinnable().getContent().getLine(idx);
			return s == null ? 0 : s.length();
		}
		
		public List<Segment> getSegments() {
			int idx = lineList.indexOf(this);
			List<Segment> segments = new ArrayList<>();
			
			String line = getSkinnable().getContent().getLine(idx);
			if( line != null ) {
				int start = getSkinnable().getContent().getOffsetAtLine(idx);
				int length = line.length();
				
				StyleRange[] ranges = getSkinnable().getStyleRanges(start, length, true);
				if( ranges == null ) {
					return Collections.emptyList();
				}
				for( StyleRange r : ranges ) {
					int begin = r.start-start;
					int end = r.start-start+r.length;
					Segment seg = new Segment();
					seg.text = removeLineending(line.substring(begin, end));
					seg.style = r;
					segments.add(seg);
				}	
			}
			
			return segments;
		}
	}
	
	class Segment {
		public String text;
		public StyleRange style;
		
		@Override
		public String toString() {
			return text + " => " + style;
		}
	}
	
	static String removeLineending(String s) {
		return s.replace("\n","").replace("\r", "");
	}

}