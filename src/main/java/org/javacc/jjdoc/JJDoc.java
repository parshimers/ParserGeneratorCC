/* Copyright (c) 2006, Sun Microsystems, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Sun Microsystems, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.javacc.jjdoc;

import java.util.Iterator;
import java.util.List;

import org.javacc.parser.*;

/**
 * The main entry point for JJDoc.
 */
public class JJDoc extends JJDocGlobals
{

  static void start ()
  {
    generator = getGenerator ();
    generator.documentStart ();
    emitTokenProductions (generator, rexprlist);
    emitNormalProductions (generator, bnfproductions);
    generator.documentEnd ();
  }

  private static Token getPrecedingSpecialToken (final Token tok)
  {
    Token t = tok;
    while (t.specialToken != null)
    {
      t = t.specialToken;
    }
    return (t != tok) ? t : null;
  }

  private static void emitTopLevelSpecialTokens (Token tok, final Generator gen)
  {
    if (tok == null)
    {
      // Strange ...
      return;
    }
    tok = getPrecedingSpecialToken (tok);
    String s = "";
    if (tok != null)
    {
      cline = tok.beginLine;
      ccol = tok.beginColumn;
      while (tok != null)
      {
        s += printTokenOnly (tok);
        tok = tok.next;
      }
    }
    if (!s.equals (""))
      gen.specialTokens (s);
  }

  /*
   * private static boolean toplevelExpansion(Expansion exp) { return exp.parent
   * != null && ( (exp.parent instanceof NormalProduction) || (exp.parent
   * instanceof TokenProduction) ); }
   */

  private static void emitTokenProductions (final Generator gen, final List prods)
  {
    gen.tokensStart ();
    // FIXME there are many empty productions here
    for (final Iterator it = prods.iterator (); it.hasNext ();)
    {
      final TokenProduction tp = (TokenProduction) it.next ();
      emitTopLevelSpecialTokens (tp.firstToken, gen);

      gen.handleTokenProduction (tp);

      // if (!token.equals("")) {
      // gen.tokenStart(tp);
      // String token = getStandardTokenProductionText(tp);
      // gen.text(token);
      // gen.tokenEnd(tp);
      // }
    }
    gen.tokensEnd ();
  }

  public static String getStandardTokenProductionText (final TokenProduction tp)
  {
    String token = "";
    if (tp.isExplicit)
    {
      if (tp.lexStates == null)
      {
        token += "<*> ";
      }
      else
      {
        token += "<";
        for (int i = 0; i < tp.lexStates.length; ++i)
        {
          token += tp.lexStates[i];
          if (i < tp.lexStates.length - 1)
          {
            token += ",";
          }
        }
        token += "> ";
      }
      token += TokenProduction.kindImage[tp.kind];
      if (tp.ignoreCase)
      {
        token += " [IGNORE_CASE]";
      }
      token += " : {\n";
      for (final Iterator it2 = tp.respecs.iterator (); it2.hasNext ();)
      {
        final RegExprSpec res = (RegExprSpec) it2.next ();

        token += emitRE (res.rexp);

        if (res.nsTok != null)
        {
          token += " : " + res.nsTok.image;
        }

        token += "\n";
        if (it2.hasNext ())
        {
          token += "| ";
        }
      }
      token += "}\n\n";
    }
    return token;
  }

  private static void emitNormalProductions (final Generator gen, final List prods)
  {
    gen.nonterminalsStart ();
    for (final Iterator it = prods.iterator (); it.hasNext ();)
    {
      final NormalProduction np = (NormalProduction) it.next ();
      emitTopLevelSpecialTokens (np.getFirstToken (), gen);
      if (np instanceof BNFProduction)
      {
        gen.productionStart (np);
        if (np.getExpansion () instanceof Choice)
        {
          boolean first = true;
          final Choice c = (Choice) np.getExpansion ();
          for (final Object aElement : c.getChoices ())
          {
            final Expansion e = (Expansion) (aElement);
            gen.expansionStart (e, first);
            emitExpansionTree (e, gen);
            gen.expansionEnd (e, first);
            first = false;
          }
        }
        else
        {
          gen.expansionStart (np.getExpansion (), true);
          emitExpansionTree (np.getExpansion (), gen);
          gen.expansionEnd (np.getExpansion (), true);
        }
        gen.productionEnd (np);
      }
      else
        if (np instanceof CppCodeProduction)
        {
          gen.cppcode ((CppCodeProduction) np);
        }
        else
          if (np instanceof JavaCodeProduction)
          {
            gen.javacode ((JavaCodeProduction) np);
          }
    }
    gen.nonterminalsEnd ();
  }

  private static void emitExpansionTree (final Expansion exp, final Generator gen)
  {
    // gen.text("[->" + exp.getClass().getName() + "]");
    if (exp instanceof Action)
    {
      emitExpansionAction ((Action) exp, gen);
    }
    else
      if (exp instanceof Choice)
      {
        emitExpansionChoice ((Choice) exp, gen);
      }
      else
        if (exp instanceof Lookahead)
        {
          emitExpansionLookahead ((Lookahead) exp, gen);
        }
        else
          if (exp instanceof NonTerminal)
          {
            emitExpansionNonTerminal ((NonTerminal) exp, gen);
          }
          else
            if (exp instanceof OneOrMore)
            {
              emitExpansionOneOrMore ((OneOrMore) exp, gen);
            }
            else
              if (exp instanceof RegularExpression)
              {
                emitExpansionRegularExpression ((RegularExpression) exp, gen);
              }
              else
                if (exp instanceof Sequence)
                {
                  emitExpansionSequence ((Sequence) exp, gen);
                }
                else
                  if (exp instanceof TryBlock)
                  {
                    emitExpansionTryBlock ((TryBlock) exp, gen);
                  }
                  else
                    if (exp instanceof ZeroOrMore)
                    {
                      emitExpansionZeroOrMore ((ZeroOrMore) exp, gen);
                    }
                    else
                      if (exp instanceof ZeroOrOne)
                      {
                        emitExpansionZeroOrOne ((ZeroOrOne) exp, gen);
                      }
                      else
                      {
                        error ("Oops: Unknown expansion type.");
                      }
    // gen.text("[<-" + exp.getClass().getName() + "]");
  }

  private static void emitExpansionAction (final Action a, final Generator gen)
  {}

  private static void emitExpansionChoice (final Choice c, final Generator gen)
  {
    for (final Iterator it = c.getChoices ().iterator (); it.hasNext ();)
    {
      final Expansion e = (Expansion) (it.next ());
      emitExpansionTree (e, gen);
      if (it.hasNext ())
      {
        gen.text (" | ");
      }
    }
  }

  private static void emitExpansionLookahead (final Lookahead l, final Generator gen)
  {}

  private static void emitExpansionNonTerminal (final NonTerminal nt, final Generator gen)
  {
    gen.nonTerminalStart (nt);
    gen.text (nt.getName ());
    gen.nonTerminalEnd (nt);
  }

  private static void emitExpansionOneOrMore (final OneOrMore o, final Generator gen)
  {
    gen.text ("( ");
    emitExpansionTree (o.expansion, gen);
    gen.text (" )+");
  }

  private static void emitExpansionRegularExpression (final RegularExpression r, final Generator gen)
  {
    final String reRendered = emitRE (r);
    if (!reRendered.equals (""))
    {
      gen.reStart (r);
      gen.text (reRendered);
      gen.reEnd (r);
    }
  }

  private static void emitExpansionSequence (final Sequence s, final Generator gen)
  {
    boolean firstUnit = true;
    for (final Object aElement : s.units)
    {
      final Expansion e = (Expansion) aElement;
      if (e instanceof Lookahead || e instanceof Action)
      {
        continue;
      }
      if (!firstUnit)
      {
        gen.text (" ");
      }
      final boolean needParens = (e instanceof Choice) || (e instanceof Sequence);
      if (needParens)
      {
        gen.text ("( ");
      }
      emitExpansionTree (e, gen);
      if (needParens)
      {
        gen.text (" )");
      }
      firstUnit = false;
    }
  }

  private static void emitExpansionTryBlock (final TryBlock t, final Generator gen)
  {
    final boolean needParens = t.exp instanceof Choice;
    if (needParens)
    {
      gen.text ("( ");
    }
    emitExpansionTree (t.exp, gen);
    if (needParens)
    {
      gen.text (" )");
    }
  }

  private static void emitExpansionZeroOrMore (final ZeroOrMore z, final Generator gen)
  {
    gen.text ("( ");
    emitExpansionTree (z.expansion, gen);
    gen.text (" )*");
  }

  private static void emitExpansionZeroOrOne (final ZeroOrOne z, final Generator gen)
  {
    gen.text ("( ");
    emitExpansionTree (z.expansion, gen);
    gen.text (" )?");
  }

  public static String emitRE (final RegularExpression re)
  {
    String returnString = "";
    final boolean hasLabel = !re.label.equals ("");
    final boolean justName = re instanceof RJustName;
    final boolean eof = re instanceof REndOfFile;
    final boolean isString = re instanceof RStringLiteral;
    final boolean toplevelRE = (re.tpContext != null);
    final boolean needBrackets = justName || eof || hasLabel || (!isString && toplevelRE);
    if (needBrackets)
    {
      returnString += "<";
      if (!justName)
      {
        if (re.private_rexp)
        {
          returnString += "#";
        }
        if (hasLabel)
        {
          returnString += re.label;
          returnString += ": ";
        }
      }
    }
    if (re instanceof RCharacterList)
    {
      final RCharacterList cl = (RCharacterList) re;
      if (cl.negated_list)
      {
        returnString += "~";
      }
      returnString += "[";
      for (final Iterator it = cl.descriptors.iterator (); it.hasNext ();)
      {
        final Object o = it.next ();
        if (o instanceof SingleCharacter)
        {
          returnString += "\"";
          final char s[] = { ((SingleCharacter) o).ch };
          returnString += add_escapes (new String (s));
          returnString += "\"";
        }
        else
          if (o instanceof CharacterRange)
          {
            returnString += "\"";
            final char s[] = { ((CharacterRange) o).getLeft () };
            returnString += add_escapes (new String (s));
            returnString += "\"-\"";
            s[0] = ((CharacterRange) o).getRight ();
            returnString += add_escapes (new String (s));
            returnString += "\"";
          }
          else
          {
            error ("Oops: unknown character list element type.");
          }
        if (it.hasNext ())
        {
          returnString += ",";
        }
      }
      returnString += "]";
    }
    else
      if (re instanceof RChoice)
      {
        final RChoice c = (RChoice) re;
        for (final Iterator it = c.getChoices ().iterator (); it.hasNext ();)
        {
          final RegularExpression sub = (RegularExpression) (it.next ());
          returnString += emitRE (sub);
          if (it.hasNext ())
          {
            returnString += " | ";
          }
        }
      }
      else
        if (re instanceof REndOfFile)
        {
          returnString += "EOF";
        }
        else
          if (re instanceof RJustName)
          {
            final RJustName jn = (RJustName) re;
            returnString += jn.label;
          }
          else
            if (re instanceof ROneOrMore)
            {
              final ROneOrMore om = (ROneOrMore) re;
              returnString += "(";
              returnString += emitRE (om.regexpr);
              returnString += ")+";
            }
            else
              if (re instanceof RSequence)
              {
                final RSequence s = (RSequence) re;
                for (final Iterator it = s.units.iterator (); it.hasNext ();)
                {
                  final RegularExpression sub = (RegularExpression) (it.next ());
                  boolean needParens = false;
                  if (sub instanceof RChoice)
                  {
                    needParens = true;
                  }
                  if (needParens)
                  {
                    returnString += "(";
                  }
                  returnString += emitRE (sub);
                  if (needParens)
                  {
                    returnString += ")";
                  }
                  if (it.hasNext ())
                  {
                    returnString += " ";
                  }
                }
              }
              else
                if (re instanceof RStringLiteral)
                {
                  final RStringLiteral sl = (RStringLiteral) re;
                  returnString += ("\"" + JavaCCGlobals.add_escapes (sl.image) + "\"");
                }
                else
                  if (re instanceof RZeroOrMore)
                  {
                    final RZeroOrMore zm = (RZeroOrMore) re;
                    returnString += "(";
                    returnString += emitRE (zm.regexpr);
                    returnString += ")*";
                  }
                  else
                    if (re instanceof RZeroOrOne)
                    {
                      final RZeroOrOne zo = (RZeroOrOne) re;
                      returnString += "(";
                      returnString += emitRE (zo.regexpr);
                      returnString += ")?";
                    }
                    else
                      if (re instanceof RRepetitionRange)
                      {
                        final RRepetitionRange zo = (RRepetitionRange) re;
                        returnString += "(";
                        returnString += emitRE (zo.regexpr);
                        returnString += ")";
                        returnString += "{";
                        if (zo.hasMax)
                        {
                          returnString += zo.min;
                          returnString += ",";
                          returnString += zo.max;
                        }
                        else
                        {
                          returnString += zo.min;
                        }
                        returnString += "}";
                      }
                      else
                      {
                        error ("Oops: Unknown regular expression type.");
                      }
    if (needBrackets)
    {
      returnString += ">";
    }
    return returnString;
  }

  /*
   * private static String v2s(Vector v, boolean newLine) { String s = "";
   * boolean firstToken = true; for (Enumeration enumeration = v.elements();
   * enumeration.hasMoreElements();) { Token tok =
   * (Token)enumeration.nextElement(); Token stok =
   * getPrecedingSpecialToken(tok); if (firstToken) { if (stok != null) { cline
   * = stok.beginLine; ccol = stok.beginColumn; } else { cline = tok.beginLine;
   * ccol = tok.beginColumn; } s = ws(ccol - 1); firstToken = false; } while
   * (stok != null) { s += printToken(stok); stok = stok.next; } s +=
   * printToken(tok); } return s; }
   */
  /**
   * A utility to produce a string of blanks.
   */

  /*
   * private static String ws(int len) { String s = ""; for (int i = 0; i < len;
   * ++i) { s += " "; } return s; }
   */

}
