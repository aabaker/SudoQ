/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Haiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.model.solverGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.sudoq.model.solverGenerator.solver.ComplexityRelation;
import de.sudoq.model.solverGenerator.solver.Solver;
import de.sudoq.model.solverGenerator.solver.SolverSudoku;
import de.sudoq.model.solverGenerator.transformations.Transformer;
import de.sudoq.model.sudoku.Constraint;
import de.sudoq.model.sudoku.Field;
import de.sudoq.model.sudoku.Position;
import de.sudoq.model.sudoku.PositionMap;
import de.sudoq.model.sudoku.Sudoku;
import de.sudoq.model.sudoku.SudokuBuilder;
import de.sudoq.model.sudoku.complexity.Complexity;
import de.sudoq.model.sudoku.complexity.ComplexityConstraint;
import de.sudoq.model.sudoku.sudokuTypes.StandardSudokuType;
import de.sudoq.model.sudoku.sudokuTypes.StandardSudokuType16x16;
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes;
import de.sudoq.model.sudoku.sudokuTypes.TypeStandard;

/**
 * Diese Klasse stellt verschiedene Methoden zum Erstellen eines validen, neuen
 * Sudokus zur Verfügung. Dazu gibt es sowohl die Möglichkeit ein gänzlich neues
 * Sudoku mit einer spezifizierten Schwierigkeit erzeugen zu lassen, als auch
 * ein vorhandenes Sudoku durch Transformationen in ein Äquivalentes überführen
 * zu lassen.
 * 
 * @see Sudoku
 * @see Solver
 */
public class Generator {
	/** Attributes */

	private Random random;

	/** Constructors */

	/**
	 * Initiiert ein neues Generator-Objekt.
	 */
	public Generator() {
		random = new Random();
	}

	/** Methods */

	/**
	 * Erzeugt ein Sudoku entsprechend dem spezifizierten Typ und fügt dieses
	 * zusammen mit dem spezifizierten Callback-Objekt der Warteschlage zur
	 * Generierung von Sudokus hinzu. Zusätzlich wird dessen Schwierigkeit wie
	 * spezifiziert gesetzt. Ist die Warteschlange leer und läuft aktuell keine
	 * Generierung, so wird die Generierung dieses Sudokus sofort gestartet.
	 * Andernfalls wird die Generierung nach Beendigung aller in der
	 * Warteschlange befindlichen Sudokus gestartet.
	 * 
	 * Ist das spezifizierte SudokuType-Objekt oder das GeneratorCallback-Objekt
	 * null, oder hat das Complexity-Argument einen ungültigen Wert, so wird
	 * false zurückgegeben. Ansonsten ist der Rückgabewert true.
	 * 
	 * @param type
	 *            Der SudokuTypes-Enum Wert, aus welchem ein Sudoku erstellt und
	 *            generiert werden soll
	 * @param complexity
	 *            Die Komplexität des zu erstellenden Sudokus
	 * @param callbackObject
	 *            Das Objekt, dessen Callback-Methode aufgerufen werden soll,
	 *            sobald der Generator fertig ist
	 * @return true, falls ein leeres Sudoku erzeugt und der Warteschlange
	 *         hinzugefügt werden konnte, false andernfalls
	 */
	public boolean generate(SudokuTypes type, Complexity complexity, GeneratorCallback callbackObject) {
		if (type == null || complexity == null || callbackObject == null)
			return false;

		// Create sudoku
		Sudoku sudoku = new SudokuBuilder(type).createSudoku();
		sudoku.setComplexity(complexity);

		if (sudoku.getSudokuType() instanceof StandardSudokuType
				|| sudoku.getSudokuType() instanceof StandardSudokuType16x16) {
			new Thread(new SudokuGenerationStandardType(sudoku, callbackObject, random)).start();
		} else {
			new Thread(new SudokuGeneration(sudoku, callbackObject, random)).start();
		}

		// Initiate new random object
		random = new Random();

		return true;
	}

	/**
	 * NUR ZU DEBUG-ZWECKEN: Setzt das Random-Objekt dieses Sudokus, um einen
	 * reproduzierbaren, deterministischen Ablauf des Generator zu provozieren.
	 * Das Random-Objekt muss vor jedem Aufruf der generate-Methode neu gesetzt
	 * werden.
	 * 
	 * @param rnd
	 *            Das zu setzende random Objekt.
	 */
	void setRandom(Random rnd) {
		this.random = rnd;
	}

	/**
	 * Bietet die Möglichkeit Sudokus abgeleitet vom Typ {@link TypeStandard} zu
	 * genrieren. Für diese ist der Algorithmus wesentlich schneller als der von
	 * {@link SudokuGeneration}. Die Klasse implementiert das {@link Runnable}
	 * interface und kann daher in einem eigenen Thread ausgeführt werden.
	 */
	private class SudokuGenerationStandardType implements Runnable {

		/**
		 * Das Sudoku auf welchem die Generierung ausgeführt wird
		 */
		private Sudoku sudoku;

		/**
		 * Der Solver, der für Validierungsvorgänge genutzt wird
		 */
		private Solver solver;

		/**
		 * Das Objekt, auf dem nach Abschluss der Generierung die
		 * Callback-Methode aufgerufen wird
		 */
		private GeneratorCallback callbackObject;

		/**
		 * Eine Liste der aktuell definierten Felder
		 */
		private List<Position> definedFields;

		/**
		 * Die noch freien, also nicht belegten Felder des Sudokus
		 */
		private List<Position> freeFields;

		/**
		 * Das Zufallsobjekt für den Generator
		 */
		private Random random;

		/**
		 * Das gelöste Sudoku
		 */
		private Sudoku solvedSudoku;

		/**
		 * Definierte Felder
		 */
		private int definedOnes;

		/**
		 * Instanziiert ein neues Generierungsobjekt für das spezifizierte
		 * Sudoku. Da die Klasse privat ist wird keine Überprüfung der
		 * Eingabeparameter durchgeführt.
		 * 
		 * @param sudoku
		 *            Das Sudoku, auf dem die Generierung ausgeführt werden soll
		 * @param callbackObject
		 *            Das Objekt, auf dem die Callback-Methode nach Abschluss
		 *            der Generierung aufgerufen werden soll
		 * @param random
		 *            Das Zufallsobjekt zur Erzeugung des Sudokus
		 */
		public SudokuGenerationStandardType(Sudoku sudoku, GeneratorCallback callbackObject, Random random) {
			this.sudoku = sudoku;
			this.callbackObject = callbackObject;
			this.solver = new Solver(sudoku);
			this.freeFields = new ArrayList<Position>();
			this.definedFields = new ArrayList<Position>();
			this.random = random;
		}

		/**
		 * Die Methode, die die tatsächliche Generierung eines Sudokus mit der
		 * gewünschten Komplexität generiert.
		 */
		public void run() {
			// Reset, if it takes too long
			// Create template sudoku of defined type and transform it
			SudokuBuilder sub = new SudokuBuilder(sudoku.getSudokuType());
			PositionMap<Integer> solution = new PositionMap<Integer>(sudoku.getSudokuType().getSize());
			int sqrtSymbolNumber = (int) Math.sqrt(sudoku.getSudokuType().getNumberOfSymbols());
			for (int x = 0; x < sudoku.getSudokuType().getSize().getX(); x++) {
				for (int y = 0; y < sudoku.getSudokuType().getSize().getY(); y++) {
					sub.addSolution(Position.get(x, y),
							((y % sqrtSymbolNumber) * sqrtSymbolNumber + x + (y / sqrtSymbolNumber))
									% (sqrtSymbolNumber * sqrtSymbolNumber));
				}
			}
			solvedSudoku = sub.createSudoku();
			Transformer.transform(solvedSudoku);

			for (int x = 0; x < sudoku.getSudokuType().getSize().getX(); x++) {
				for (int y = 0; y < sudoku.getSudokuType().getSize().getY(); y++) {
					solution.put(Position.get(x, y), solvedSudoku.getField(Position.get(x, y)).getSolution());
				}
			}

			// Fill the sudoku being generated with template solutions

			for (int x = 0; x < sudoku.getSudokuType().getSize().getX(); x++) {
				for (int y = 0; y < sudoku.getSudokuType().getSize().getY(); y++) {
					sudoku.getField(Position.get(x, y)).setCurrentValue(
							solvedSudoku.getField(Position.get(x, y)).getSolution(), false);
					this.definedFields.add(Position.get(x, y));
				}
			}

			ArrayList<Constraint> constraints = sudoku.getSudokuType().getConstraints();
			ArrayList<Position> positions;
			boolean emptyOne = false;
			for (int i = 0; i < constraints.size(); i++) {
				positions = constraints.get(i).getPositions();
				emptyOne = false;
				for (int j = 0; j < positions.size(); j++) {
					if (sudoku.getField(positions.get(j)).isEmpty()) {
						emptyOne = true;
						break;
					}
				}
				if (!emptyOne) {
					int nr = random.nextInt(positions.size());
					sudoku.getField(positions.get(nr)).setCurrentValue(Field.EMPTYVAL, false);
					definedFields.remove(positions.get(nr));
					freeFields.add(positions.get(nr));
					definedOnes++;
				}
			}

			ComplexityConstraint constr = sudoku.getSudokuType().buildComplexityConstraint(sudoku.getComplexity());

			int nr = random.nextInt(definedFields.size());
			int counter = definedFields.size();
			while (counter >= 0 && definedFields.size() > constr.getAverageFields()) {
				counter--;
				sudoku.getField(definedFields.get(nr)).setCurrentValue(Field.EMPTYVAL, false);
				((SolverSudoku) solver.getSudoku()).resetCandidates();
				if (((SolverSudoku) solver.getSudoku()).getCurrentCandidates(definedFields.get(nr)).cardinality() != 1) {
					sudoku.getField(definedFields.get(nr)).setCurrentValue(
							solvedSudoku.getField(definedFields.get(nr)).getSolution(), false);
					nr = (nr + 1) % definedFields.size();
				} else {
					freeFields.add(definedFields.remove(nr));
					definedOnes++;
					if (nr >= definedFields.size())
						nr = 0;
				}
			}

			// ArrayList<Constraint> constraints =
			// sudoku.getSudokuType().getConstraints();
			// ArrayList<Position> positions;
			// for (int i = 0; i < constraints.size(); i++) {
			// positions = constraints.get(i).getPositions();
			// int nr = random.nextInt(positions.size());
			// sudoku.getField(positions.get(nr)).setCurrentValue(
			// solvedSudoku.getField(positions.get(nr)).getSolution(), false);
			// freeFields.remove(positions.get(nr));
			// definedFields.add(positions.get(nr));
			// }

			int allocationFactor = sudoku.getSudokuType().getSize().getX() * sudoku.getSudokuType().getSize().getY()
					/ 20;

			ComplexityRelation rel = ComplexityRelation.INVALID;
			while (rel != ComplexityRelation.CONSTRAINT_SATURATION) {
				// while (definedFields.size() > constr.getAverageFields()) {
				// removeDefinedField();
				// }

				rel = solver.validate(solution, true);

				switch (rel) {
				case MUCH_TO_EASY:
					for (int i = 0; i < allocationFactor / 2 && removeDefinedField(); i++) {
					}
					break;
				case TO_EASY:
					removeDefinedField();
					break;
				case INVALID:
				case MUCH_TO_DIFFICULT:
					for (int i = 0; i < allocationFactor && addDefinedField(); i++) {
					}
					break;
				case TO_DIFFICULT:
					addDefinedField();
					break;
				}
			}

			// Call the callback
			SudokuBuilder suBi = new SudokuBuilder(sudoku.getSudokuType());
			Position currentPos = null;
			for (int x = 0; x < sudoku.getSudokuType().getSize().getX(); x++) {
				for (int y = 0; y < sudoku.getSudokuType().getSize().getY(); y++) {
					currentPos = Position.get(x, y);
					if (solvedSudoku.getField(currentPos) != null) {
						int value = solvedSudoku.getField(currentPos).getSolution();
						if (!sudoku.getField(currentPos).isEmpty())
							suBi.setFixed(currentPos);
						suBi.addSolution(currentPos, value);
					}
				}
			}
			Sudoku res = suBi.createSudoku();
			res.setComplexity(sudoku.getComplexity());
			callbackObject.generationFinished(res);
		}

		private boolean addDefinedField() {
			if (freeFields.isEmpty()) return false;
			else if (freeFields.size() == definedOnes) {
				definedOnes--;
			}
			Position p = freeFields.remove(random.nextInt(freeFields.size() - definedOnes) + definedOnes);
			sudoku.getField(p).setCurrentValue(solvedSudoku.getField(p).getSolution(), false);
			definedFields.add(p);
			return true;

			// int y = random.nextInt(sudoku.getSudokuType().getSize().getY());
			//
			// int counter = sudoku.getSudokuType().getSize().getX() *
			// sudoku.getSudokuType().getSize().getY();
			// while (!markings[x][y] && counter >= 0) {
			// x = (x + 1) % sudoku.getSudokuType().getSize().getX();
			// y = x == 0 ? (y + 1) % sudoku.getSudokuType().getSize().getY() :
			// y;
			// counter--;
			// }
			//
			// if (counter != -1) {
			// sudoku.getField(Position.get(x, y)).setCurrentValue(
			// solvedSudoku.getField(Position.get(x, y)).getSolution(), false);
			// markings[x][y] = false;
			// return true;
			// } else {
			// return false;
			// }
		}

		/**
		 * Entfernt eines der definierten Felder.
		 * 
		 * @return Die Position des entfernten Feldes
		 */
		private boolean removeDefinedField() {
			if (definedFields.isEmpty())
				return false;
			Position p = null;
			p = definedFields.remove(random.nextInt(definedFields.size()));
			sudoku.getField(p).setCurrentValue(Field.EMPTYVAL, false);
			freeFields.add(p);
			return true;

			// int x = random.nextInt(sudoku.getSudokuType().getSize().getX());
			// int y = random.nextInt(sudoku.getSudokuType().getSize().getY());
			//
			// int counter = sudoku.getSudokuType().getSize().getX() *
			// sudoku.getSudokuType().getSize().getY();
			// while (markings[x][y] && counter >= 0) {
			// x = (x + 1) % sudoku.getSudokuType().getSize().getX();
			// y = x == 0 ? (y + 1) % sudoku.getSudokuType().getSize().getY() :
			// y;
			// counter--;
			// }
			//
			// if (counter != -1) {
			// sudoku.getField(Position.get(x,
			// y)).setCurrentValue(Field.EMPTYVAL, false);
			// markings[x][y] = true;
			// return true;
			// } else {
			// return false;
			// }
		}

	}

	/**
	 * Bietet die Möglichkeit Sudokus abgeleitet vom Typ {@link TypeStandard} zu
	 * genrieren. Für diese ist der Algorithmus wesentlich schneller als der von
	 * {@link SudokuGeneration}. Die Klasse implementiert das {@link Runnable}
	 * interface und kann daher in einem eigenen Thread ausgeführt werden.
	 */
	private class SudokuGeneration implements Runnable {

		/**
		 * Das Sudoku auf welchem die Generierung ausgeführt wird
		 */
		private Sudoku sudoku;

		/**
		 * Das Zufallsobjekt auf dem der Generator arbeitet.
		 */
		private Random random;

		/**
		 * Die Anzahl der Felder, die fest zu definieren ist
		 */
		private int fieldsToDefine;

		/**
		 * Ein Array von Markierungen zum Testen, welches Felder belegt werden
		 * können
		 */
		boolean[][] markings;

		/**
		 * Anzahl aktuell definierter Felder
		 */
		private int currentFieldsDefined;

		/**
		 * Der Solver, der für Validierungsvorgänge genutzt wird
		 */
		private Solver solver;

		/**
		 * Das Objekt, auf dem nach Abschluss der Generierung die
		 * Callback-Methode aufgerufen wird
		 */
		private GeneratorCallback callbackObject;

		/**
		 * Eine Liste der aktuell definierten Felder
		 */
		private List<Position> definedFields;

		/**
		 * Die noch freien, also nicht belegten Felder des Sudokus
		 */
		private List<Position> freeFields;

		/**
		 * ComplexityConstraint für ein Sudoku des definierten
		 * Schwierigkeitsgrades
		 */
		private ComplexityConstraint currentConstraint;

		/**
		 * Das gelöste Sudoku
		 */
		private Sudoku solvedSudoku;

		/**
		 * Instanziiert ein neues Generierungsobjekt für das spezifizierte
		 * Sudoku. Da die Klasse privat ist wird keine Überprüfung der
		 * Eingabeparameter durchgeführt.
		 * 
		 * @param sudoku
		 *            Das Sudoku, auf dem die Generierung ausgeführt werden soll
		 * @param callbackObject
		 *            Das Objekt, auf dem die Callback-Methode nach Abschluss
		 *            der Generierung aufgerufen werden soll
		 * @param random
		 *            Das Zufallsobjekt zur Erzeugung des Sudokus
		 */
		public SudokuGeneration(Sudoku sudoku, GeneratorCallback callbackObject, Random random) {
			this.sudoku = sudoku;
			this.callbackObject = callbackObject;
			this.solver = new Solver(sudoku);
			this.freeFields = new ArrayList<Position>();
			this.definedFields = new ArrayList<Position>();
			this.random = random;

			this.currentConstraint = sudoku.getSudokuType().buildComplexityConstraint(sudoku.getComplexity());

			for (int y = 0; y < this.sudoku.getSudokuType().getSize().getY(); y++) {
				for (int x = 0; x < this.sudoku.getSudokuType().getSize().getX(); x++) {
					if (this.sudoku.getField(Position.get(x, y)) != null)
						freeFields.add(Position.get(x, y));
				}
			}
		}

		/**
		 * Die Methode, die die tatsächliche Generierung eines Sudokus mit der
		 * gewünschten Komplexität generiert.
		 */
		public void run() {
			boolean found = false;
			// Calculate the number of fields to be filled
			fieldsToDefine = Math.min((int) (sudoku.getSudokuType().getSize().getX()
					* sudoku.getSudokuType().getSize().getY() * sudoku.getSudokuType().getStandardAllocationFactor()),
					currentConstraint.getAverageFields());

			markings = new boolean[sudoku.getSudokuType().getSize().getX()][sudoku.getSudokuType().getSize().getY()];
			PositionMap<Integer> solution = new PositionMap<Integer>(this.sudoku.getSudokuType().getSize());
			while (!found) {
				// Remove some fields, because sudoku could not be validated
				for (int i = 0; i < 5; i++) {
					removeDefinedField();
				}
				// Define average number of fields
				while (this.currentFieldsDefined < fieldsToDefine) {
					if (addDefinedField() == null) {
						for (int j = 0; j < 5 && this.currentFieldsDefined > 0; j++) {
							removeDefinedField();
						}
					}
				}

				// Try to fill sudoku to have complexity constraint saturation
				found = solver.solveAll(false, false);
			}
			// System.out.println("Found one");

			Complexity saveCompl = solver.getSudoku().getComplexity();
			solver.getSudoku().setComplexity(Complexity.arbitrary);
			solver.validate(solution, false);
			solver.getSudoku().setComplexity(saveCompl);

			// Create the sudoku template generated before
			SudokuBuilder sub = new SudokuBuilder(sudoku.getSudokuType());
			for (int x = 0; x < sudoku.getSudokuType().getSize().getX(); x++) {
				for (int y = 0; y < sudoku.getSudokuType().getSize().getY(); y++) {
					if (sudoku.getField(Position.get(x, y)) != null)
						sub.addSolution(Position.get(x, y), solution.get(Position.get(x, y)));
				}
			}
			solvedSudoku = sub.createSudoku();

			while (!this.freeFields.isEmpty()) {
				this.definedFields.add(this.freeFields.remove(0));
			}

			// Fill the sudoku being generated with template solutions
			for (int x = 0; x < sudoku.getSudokuType().getSize().getX(); x++) {
				for (int y = 0; y < sudoku.getSudokuType().getSize().getY(); y++) {
					if (sudoku.getField(Position.get(x, y)) != null) {
						sudoku.getField(Position.get(x, y)).setCurrentValue(
								solvedSudoku.getField(Position.get(x, y)).getSolution(), false);
					}
				}
			}

			int allocationFactor = sudoku.getSudokuType().getNumberOfSymbols()
					* sudoku.getSudokuType().getNumberOfSymbols() / 20;
			allocationFactor = allocationFactor == 0 ? 1 : allocationFactor;

			ComplexityRelation rel = ComplexityRelation.INVALID;
			while (rel != ComplexityRelation.CONSTRAINT_SATURATION) {
				rel = solver.validate(null, false);

				if (rel == ComplexityRelation.MUCH_TO_EASY) {
					for (int i = 0; i < allocationFactor; i++)
						removeDefinedField();
				} else if (rel == ComplexityRelation.TO_EASY) {
					removeDefinedField();
				} else if (rel == ComplexityRelation.INVALID || rel == ComplexityRelation.TO_DIFFICULT
						|| rel == ComplexityRelation.MUCH_TO_DIFFICULT) {
					for (int i = 0; i < allocationFactor && !freeFields.isEmpty(); i++) {
						Position p = freeFields.remove(0);
						sudoku.getField(p).setCurrentValue(solvedSudoku.getField(p).getSolution(), false);
						definedFields.add(p);
					}
				}
			}

			// Call the callback
			SudokuBuilder suBi = new SudokuBuilder(sudoku.getSudokuType());
			Position currentPos = null;
			for (int x = 0; x < sudoku.getSudokuType().getSize().getX(); x++) {
				for (int y = 0; y < sudoku.getSudokuType().getSize().getY(); y++) {
					currentPos = Position.get(x, y);
					if (solvedSudoku.getField(currentPos) != null) {
						int value = solvedSudoku.getField(currentPos).getSolution();
						if (!sudoku.getField(currentPos).isEmpty())
							suBi.setFixed(currentPos);
						suBi.addSolution(currentPos, value);
					}
				}
			}
			Sudoku res = suBi.createSudoku();
			res.setComplexity(sudoku.getComplexity());
			callbackObject.generationFinished(res);
		}

		/**
		 * Definiert ein weiteres Feld, sodass weiterhin Constraint Saturation
		 * vorhanden ist. Die Position des definierten Feldes wird
		 * zurückgegeben. Kann keines gefunden werden, so wird null
		 * zurückgegeben.
		 * 
		 * @return Die Position des definierten Feldes oder null, falls keines
		 *         gefunden wurde
		 */

		private Position addDefinedField() {
			// if (freeFields.isEmpty())
			// return null;
			//
			// Position p =
			// freeFields.remove(random.nextInt(freeFields.size()));
			// if (solvedSudoku != null) {
			// sudoku.getField(p).setCurrentValue(solvedSudoku.getField(p).getSolution(),
			// false);
			// } else {
			// boolean valid = false;
			// int offset =
			// random.nextInt(sudoku.getSudokuType().getNumberOfSymbols());
			// for (int j = 0; j < sudoku.getSudokuType().getNumberOfSymbols();
			// j++) {
			// sudoku.getField(p).setCurrentValue((j + offset) %
			// sudoku.getSudokuType().getNumberOfSymbols(),
			// false);
			// valid = true;
			// if (!sudoku.getSudokuType().checkSudoku(sudoku)) {
			// valid = false;
			// sudoku.getField(p).setCurrentValue(Field.EMPTYVAL, false);
			// } else {
			// break;
			// }
			// }
			// if (!valid) {
			// freeFields.add(p);
			// p = null;
			// }
			// }
			// if (p != null) {
			// definedFields.add(p);
			// currentFieldsDefined++;
			// }
			//
			// return p;

			int count = definedFields.size();

			for (int x = 0; x < sudoku.getSudokuType().getSize().getX(); x++) {
				for (int y = 0; y < sudoku.getSudokuType().getSize().getY(); y++) {
					markings[x][y] = false;
				}
			}

			for (int i = 0; i < this.definedFields.size(); i++) {
				count++;
				markings[this.definedFields.get(i).getX()][this.definedFields.get(i).getY()] = true;
			}

			Position p = null;

			while (p == null
					&& count != this.sudoku.getSudokuType().getSize().getX()
							* this.sudoku.getSudokuType().getSize().getY()) {
				int x = random.nextInt(sudoku.getSudokuType().getSize().getX());
				int y = random.nextInt(sudoku.getSudokuType().getSize().getY());
				if (sudoku.getField(Position.get(x, y)) == null) {
					markings[x][y] = true;
					count++;
				} else if (markings[x][y] == false) {
					markings[x][y] = true;
					count++;
					p = Position.get(x, y);
				}
			}

			boolean valid = false;
			int offset = random.nextInt(sudoku.getSudokuType().getNumberOfSymbols());
			for (int j = 0; j < sudoku.getSudokuType().getNumberOfSymbols(); j++) {
				sudoku.getField(p).setCurrentValue((j + offset) % sudoku.getSudokuType().getNumberOfSymbols(), false);
				valid = true;
				ArrayList<Constraint> constraints = this.sudoku.getSudokuType().getConstraints();
				for (int i = 0; i < constraints.size(); i++) {
					if (!constraints.get(i).isSaturated(sudoku)) {
						valid = false;
						sudoku.getField(p).setCurrentValue(Field.EMPTYVAL, false);
						break;
					}
				}
				if (valid) {
					definedFields.add(p);
					// freeFields.remove(p);
					this.currentFieldsDefined++;
					break;
				}
			}
			if (!valid)
				p = null;

			return p;
		}

		/**
		 * Entfernt eines der definierten Felder.
		 * 
		 * @return Die Position des entfernten Feldes
		 */
		private Position removeDefinedField() {
			if (definedFields.isEmpty())
				return null;
			Position p = null;
			int nr = random.nextInt(definedFields.size());
			p = definedFields.remove(nr);
			sudoku.getField(p).setCurrentValue(Field.EMPTYVAL, false);
			freeFields.add(p);
			this.currentFieldsDefined--;
			return p;
		}

	}
}