// Generated from /Users/gustavbjordal/OscaR/oscar/oscar-fzn/src/main/java/oscar/flatzinc/parser/Flatzinc.g4 by ANTLR 4.7
package oscar.flatzinc.parser;

/*******************************************************************************
  * OscaR is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation, either version 2.1 of the License, or
  * (at your option) any later version.
  *
  * OscaR is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser General Public License  for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License along with OscaR.
  * If not, see http://www.gnu.org/licenses/lgpl-3.0.en.html
  ******************************************************************************/
//package oscar.flatzinc.parser;
import oscar.flatzinc.parser.intermediatemodel.*;
import oscar.flatzinc.parser.intermediatemodel.ASTLiterals.*;
import oscar.flatzinc.parser.intermediatemodel.ASTDecls.*;
import oscar.flatzinc.parser.intermediatemodel.ASTTypes.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class FlatzincLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, T__27=28, T__28=29, T__29=30, T__30=31, 
		Boolconst=32, PREDANNID=33, VARPARID=34, Floatconst=35, INT=36, STRING=37, 
		WS=38;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "T__10", "T__11", "T__12", "T__13", "T__14", "T__15", "T__16", 
		"T__17", "T__18", "T__19", "T__20", "T__21", "T__22", "T__23", "T__24", 
		"T__25", "T__26", "T__27", "T__28", "T__29", "T__30", "Boolconst", "PREDANNID", 
		"VARPARID", "Floatconst", "INT", "NUM", "STRING", "WS"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "';'", "'predicate'", "'('", "','", "')'", "':'", "'='", "'function'", 
		"'ann'", "'let'", "'{'", "'}'", "'in'", "'constraint'", "'solve'", "'satisfy'", 
		"'minimize'", "'maximize'", "'bool'", "'float'", "'int'", "'set'", "'of'", 
		"'var'", "'-infinity..infinity'", "'array'", "'['", "']'", "'..'", "'::'", 
		"'()'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, "Boolconst", "PREDANNID", 
		"VARPARID", "Floatconst", "INT", "STRING", "WS"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public FlatzincLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Flatzinc.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2(\u012c\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\3\2\3\2\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3"+
		"\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\13"+
		"\3\f\3\f\3\r\3\r\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\3\20\3\20\3\20\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\23\3\23"+
		"\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\24\3\24\3\24\3\24\3\24\3\25\3\25"+
		"\3\25\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3\27\3\27\3\27\3\27\3\30\3\30"+
		"\3\30\3\31\3\31\3\31\3\31\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32"+
		"\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\33\3\33\3\33"+
		"\3\33\3\33\3\33\3\34\3\34\3\35\3\35\3\36\3\36\3\36\3\37\3\37\3\37\3 \3"+
		" \3 \3!\3!\3!\3!\3!\3!\3!\3!\3!\5!\u00f5\n!\3\"\3\"\7\"\u00f9\n\"\f\""+
		"\16\"\u00fc\13\"\3#\6#\u00ff\n#\r#\16#\u0100\3#\3#\3$\3$\3$\3$\3$\5$\u010a"+
		"\n$\3$\3$\3$\3$\5$\u0110\n$\3%\5%\u0113\n%\3%\3%\3&\6&\u0118\n&\r&\16"+
		"&\u0119\3\'\3\'\6\'\u011e\n\'\r\'\16\'\u011f\3\'\3\'\3(\3(\3(\6(\u0127"+
		"\n(\r(\16(\u0128\3(\3(\2\2)\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25"+
		"\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32"+
		"\63\33\65\34\67\359\36;\37= ?!A\"C#E$G%I&K\2M\'O(\3\2\b\4\2C\\c|\6\2\62"+
		";C\\aac|\4\2GGgg\4\2--//\3\2$$\4\2\13\f\"\"\2\u0134\2\3\3\2\2\2\2\5\3"+
		"\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2"+
		"\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3"+
		"\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'"+
		"\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63"+
		"\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2"+
		"?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2M\3"+
		"\2\2\2\2O\3\2\2\2\3Q\3\2\2\2\5S\3\2\2\2\7]\3\2\2\2\t_\3\2\2\2\13a\3\2"+
		"\2\2\rc\3\2\2\2\17e\3\2\2\2\21g\3\2\2\2\23p\3\2\2\2\25t\3\2\2\2\27x\3"+
		"\2\2\2\31z\3\2\2\2\33|\3\2\2\2\35\177\3\2\2\2\37\u008a\3\2\2\2!\u0090"+
		"\3\2\2\2#\u0098\3\2\2\2%\u00a1\3\2\2\2\'\u00aa\3\2\2\2)\u00af\3\2\2\2"+
		"+\u00b5\3\2\2\2-\u00b9\3\2\2\2/\u00bd\3\2\2\2\61\u00c0\3\2\2\2\63\u00c4"+
		"\3\2\2\2\65\u00d8\3\2\2\2\67\u00de\3\2\2\29\u00e0\3\2\2\2;\u00e2\3\2\2"+
		"\2=\u00e5\3\2\2\2?\u00e8\3\2\2\2A\u00f4\3\2\2\2C\u00f6\3\2\2\2E\u00fe"+
		"\3\2\2\2G\u010f\3\2\2\2I\u0112\3\2\2\2K\u0117\3\2\2\2M\u011b\3\2\2\2O"+
		"\u0126\3\2\2\2QR\7=\2\2R\4\3\2\2\2ST\7r\2\2TU\7t\2\2UV\7g\2\2VW\7f\2\2"+
		"WX\7k\2\2XY\7e\2\2YZ\7c\2\2Z[\7v\2\2[\\\7g\2\2\\\6\3\2\2\2]^\7*\2\2^\b"+
		"\3\2\2\2_`\7.\2\2`\n\3\2\2\2ab\7+\2\2b\f\3\2\2\2cd\7<\2\2d\16\3\2\2\2"+
		"ef\7?\2\2f\20\3\2\2\2gh\7h\2\2hi\7w\2\2ij\7p\2\2jk\7e\2\2kl\7v\2\2lm\7"+
		"k\2\2mn\7q\2\2no\7p\2\2o\22\3\2\2\2pq\7c\2\2qr\7p\2\2rs\7p\2\2s\24\3\2"+
		"\2\2tu\7n\2\2uv\7g\2\2vw\7v\2\2w\26\3\2\2\2xy\7}\2\2y\30\3\2\2\2z{\7\177"+
		"\2\2{\32\3\2\2\2|}\7k\2\2}~\7p\2\2~\34\3\2\2\2\177\u0080\7e\2\2\u0080"+
		"\u0081\7q\2\2\u0081\u0082\7p\2\2\u0082\u0083\7u\2\2\u0083\u0084\7v\2\2"+
		"\u0084\u0085\7t\2\2\u0085\u0086\7c\2\2\u0086\u0087\7k\2\2\u0087\u0088"+
		"\7p\2\2\u0088\u0089\7v\2\2\u0089\36\3\2\2\2\u008a\u008b\7u\2\2\u008b\u008c"+
		"\7q\2\2\u008c\u008d\7n\2\2\u008d\u008e\7x\2\2\u008e\u008f\7g\2\2\u008f"+
		" \3\2\2\2\u0090\u0091\7u\2\2\u0091\u0092\7c\2\2\u0092\u0093\7v\2\2\u0093"+
		"\u0094\7k\2\2\u0094\u0095\7u\2\2\u0095\u0096\7h\2\2\u0096\u0097\7{\2\2"+
		"\u0097\"\3\2\2\2\u0098\u0099\7o\2\2\u0099\u009a\7k\2\2\u009a\u009b\7p"+
		"\2\2\u009b\u009c\7k\2\2\u009c\u009d\7o\2\2\u009d\u009e\7k\2\2\u009e\u009f"+
		"\7|\2\2\u009f\u00a0\7g\2\2\u00a0$\3\2\2\2\u00a1\u00a2\7o\2\2\u00a2\u00a3"+
		"\7c\2\2\u00a3\u00a4\7z\2\2\u00a4\u00a5\7k\2\2\u00a5\u00a6\7o\2\2\u00a6"+
		"\u00a7\7k\2\2\u00a7\u00a8\7|\2\2\u00a8\u00a9\7g\2\2\u00a9&\3\2\2\2\u00aa"+
		"\u00ab\7d\2\2\u00ab\u00ac\7q\2\2\u00ac\u00ad\7q\2\2\u00ad\u00ae\7n\2\2"+
		"\u00ae(\3\2\2\2\u00af\u00b0\7h\2\2\u00b0\u00b1\7n\2\2\u00b1\u00b2\7q\2"+
		"\2\u00b2\u00b3\7c\2\2\u00b3\u00b4\7v\2\2\u00b4*\3\2\2\2\u00b5\u00b6\7"+
		"k\2\2\u00b6\u00b7\7p\2\2\u00b7\u00b8\7v\2\2\u00b8,\3\2\2\2\u00b9\u00ba"+
		"\7u\2\2\u00ba\u00bb\7g\2\2\u00bb\u00bc\7v\2\2\u00bc.\3\2\2\2\u00bd\u00be"+
		"\7q\2\2\u00be\u00bf\7h\2\2\u00bf\60\3\2\2\2\u00c0\u00c1\7x\2\2\u00c1\u00c2"+
		"\7c\2\2\u00c2\u00c3\7t\2\2\u00c3\62\3\2\2\2\u00c4\u00c5\7/\2\2\u00c5\u00c6"+
		"\7k\2\2\u00c6\u00c7\7p\2\2\u00c7\u00c8\7h\2\2\u00c8\u00c9\7k\2\2\u00c9"+
		"\u00ca\7p\2\2\u00ca\u00cb\7k\2\2\u00cb\u00cc\7v\2\2\u00cc\u00cd\7{\2\2"+
		"\u00cd\u00ce\7\60\2\2\u00ce\u00cf\7\60\2\2\u00cf\u00d0\7k\2\2\u00d0\u00d1"+
		"\7p\2\2\u00d1\u00d2\7h\2\2\u00d2\u00d3\7k\2\2\u00d3\u00d4\7p\2\2\u00d4"+
		"\u00d5\7k\2\2\u00d5\u00d6\7v\2\2\u00d6\u00d7\7{\2\2\u00d7\64\3\2\2\2\u00d8"+
		"\u00d9\7c\2\2\u00d9\u00da\7t\2\2\u00da\u00db\7t\2\2\u00db\u00dc\7c\2\2"+
		"\u00dc\u00dd\7{\2\2\u00dd\66\3\2\2\2\u00de\u00df\7]\2\2\u00df8\3\2\2\2"+
		"\u00e0\u00e1\7_\2\2\u00e1:\3\2\2\2\u00e2\u00e3\7\60\2\2\u00e3\u00e4\7"+
		"\60\2\2\u00e4<\3\2\2\2\u00e5\u00e6\7<\2\2\u00e6\u00e7\7<\2\2\u00e7>\3"+
		"\2\2\2\u00e8\u00e9\7*\2\2\u00e9\u00ea\7+\2\2\u00ea@\3\2\2\2\u00eb\u00ec"+
		"\7v\2\2\u00ec\u00ed\7t\2\2\u00ed\u00ee\7w\2\2\u00ee\u00f5\7g\2\2\u00ef"+
		"\u00f0\7h\2\2\u00f0\u00f1\7c\2\2\u00f1\u00f2\7n\2\2\u00f2\u00f3\7u\2\2"+
		"\u00f3\u00f5\7g\2\2\u00f4\u00eb\3\2\2\2\u00f4\u00ef\3\2\2\2\u00f5B\3\2"+
		"\2\2\u00f6\u00fa\t\2\2\2\u00f7\u00f9\t\3\2\2\u00f8\u00f7\3\2\2\2\u00f9"+
		"\u00fc\3\2\2\2\u00fa\u00f8\3\2\2\2\u00fa\u00fb\3\2\2\2\u00fbD\3\2\2\2"+
		"\u00fc\u00fa\3\2\2\2\u00fd\u00ff\7a\2\2\u00fe\u00fd\3\2\2\2\u00ff\u0100"+
		"\3\2\2\2\u0100\u00fe\3\2\2\2\u0100\u0101\3\2\2\2\u0101\u0102\3\2\2\2\u0102"+
		"\u0103\5C\"\2\u0103F\3\2\2\2\u0104\u0105\5I%\2\u0105\u0106\7\60\2\2\u0106"+
		"\u0109\5K&\2\u0107\u0108\t\4\2\2\u0108\u010a\5I%\2\u0109\u0107\3\2\2\2"+
		"\u0109\u010a\3\2\2\2\u010a\u0110\3\2\2\2\u010b\u010c\5I%\2\u010c\u010d"+
		"\t\4\2\2\u010d\u010e\5I%\2\u010e\u0110\3\2\2\2\u010f\u0104\3\2\2\2\u010f"+
		"\u010b\3\2\2\2\u0110H\3\2\2\2\u0111\u0113\t\5\2\2\u0112\u0111\3\2\2\2"+
		"\u0112\u0113\3\2\2\2\u0113\u0114\3\2\2\2\u0114\u0115\5K&\2\u0115J\3\2"+
		"\2\2\u0116\u0118\4\62;\2\u0117\u0116\3\2\2\2\u0118\u0119\3\2\2\2\u0119"+
		"\u0117\3\2\2\2\u0119\u011a\3\2\2\2\u011aL\3\2\2\2\u011b\u011d\7$\2\2\u011c"+
		"\u011e\n\6\2\2\u011d\u011c\3\2\2\2\u011e\u011f\3\2\2\2\u011f\u011d\3\2"+
		"\2\2\u011f\u0120\3\2\2\2\u0120\u0121\3\2\2\2\u0121\u0122\7$\2\2\u0122"+
		"N\3\2\2\2\u0123\u0127\t\7\2\2\u0124\u0125\7\17\2\2\u0125\u0127\7\f\2\2"+
		"\u0126\u0123\3\2\2\2\u0126\u0124\3\2\2\2\u0127\u0128\3\2\2\2\u0128\u0126"+
		"\3\2\2\2\u0128\u0129\3\2\2\2\u0129\u012a\3\2\2\2\u012a\u012b\b(\2\2\u012b"+
		"P\3\2\2\2\r\2\u00f4\u00fa\u0100\u0109\u010f\u0112\u0119\u011f\u0126\u0128"+
		"\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}