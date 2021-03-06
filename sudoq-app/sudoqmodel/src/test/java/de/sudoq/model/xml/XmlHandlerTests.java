package de.sudoq.model.xml;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.sudoq.model.files.FileManager;
import de.sudoq.model.sudoku.Sudoku;
import de.sudoq.model.sudoku.SudokuBuilder;
import de.sudoq.model.sudoku.complexity.Complexity;
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes;
import de.sudoq.model.sudoku.sudokuTypes.TypeBuilder;
import de.sudoq.model.xml.GameXmlHandler;
import de.sudoq.model.xml.SudokuXmlHandler;
import de.sudoq.model.xml.XmlHandler;
import de.sudoq.model.xml.XmlTree;
import de.sudoq.model.xml.Xmlable;

public class XmlHandlerTests {

	private static File sudokus;
	private static File profiles;

	@Before
	public void init() {
		sudokus = new File("res" + File.separator + "tmp_suds");
		profiles = new File("res" + File.separator + "tmp_profiles");
		sudokus.mkdir();
		profiles.mkdir();
		FileManager.initialize(profiles, sudokus);
	}

	@After
	public void clean() throws IOException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		FileManager.deleteDir(profiles);
		FileManager.deleteDir(sudokus);
	}

	@Test
	public void testGameXmlHandlerConstructor() {
		new GameXmlHandler();
	}

	@Test
	public void testSaveAsXmlStringIllegalArgumentException() throws SecurityException, IllegalArgumentException, IOException, NoSuchFieldException, IllegalAccessException {
		class Foo implements Xmlable {
			public XmlTree toXmlTree() {
				return new XmlTree("sudoku");
			}

			public void fillFromXml(XmlTree xmlTreeRepresentation) {
			}
		}

		XmlHandler<Foo> handler = new XmlHandler<Foo>() {
			@Override
			protected File getFileFor(Foo obj) {
				File file = new File("foo");
				try {
					file.createNewFile();
				} catch (IOException e) {
					fail("cannot write");
				}
				file.setWritable(false);
				return file;
			}
		};
		assertFalse(handler.getFileFor(new Foo()).canWrite());

		try {
			handler.saveAsXml(new Foo());
			fail("No Exception");
		} catch (IllegalArgumentException e) {
			// great
		}

		assertTrue(new File("foo").delete());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateObjectFromXmlIllegalArgumentException() throws SecurityException, IllegalArgumentException, IOException, NoSuchFieldException, IllegalAccessException {
		TypeBuilder.get99();
		Sudoku sudoku = new SudokuBuilder(SudokuTypes.standard16x16).createSudoku();
		sudoku.setComplexity(Complexity.difficult);

		new SudokuXmlHandler().createObjectFromXml(sudoku);
	}

}
