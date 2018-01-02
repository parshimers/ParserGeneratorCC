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

package com.helger.pgcc.jjdoc;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import com.helger.pgcc.parser.CodeProductionCpp;
import com.helger.pgcc.parser.Expansion;
import com.helger.pgcc.parser.CodeProductionJava;
import com.helger.pgcc.parser.NonTerminal;
import com.helger.pgcc.parser.NormalProduction;
import com.helger.pgcc.parser.RegularExpression;
import com.helger.pgcc.parser.TokenProduction;

/**
 * Output BNF in text format.
 */
public class TextGenerator implements IDocGenerator
{
  protected PrintWriter ostr;

  public TextGenerator ()
  {}

  public void text (final String s)
  {
    print (s);
  }

  public void print (final String s)
  {
    ostr.print (s);
  }

  public void documentStart ()
  {
    ostr = create_output_stream ();
    ostr.print ("\nDOCUMENT START\n");
  }

  public void documentEnd ()
  {
    ostr.print ("\nDOCUMENT END\n");
    ostr.close ();
  }

  public void specialTokens (final String s)
  {
    ostr.print (s);
  }

  public void nonterminalsStart ()
  {
    text ("NON-TERMINALS\n");
  }

  public void nonterminalsEnd ()
  {}

  public void tokensStart ()
  {
    text ("TOKENS\n");
  }

  @Override
  public void handleTokenProduction (final TokenProduction tp)
  {
    final String text = JJDoc.getStandardTokenProductionText (tp);
    text (text);
  }

  public void tokensEnd ()
  {}

  public void javacode (final CodeProductionJava jp)
  {
    productionStart (jp);
    text ("java code");
    productionEnd (jp);
  }

  public void cppcode (final CodeProductionCpp cp)
  {
    productionStart (cp);
    text ("c++ code");
    productionEnd (cp);
  }

  public void productionStart (final NormalProduction np)
  {
    ostr.print ("\t" + np.getLhs () + "\t:=\t");
  }

  public void productionEnd (final NormalProduction np)
  {
    ostr.print ("\n");
  }

  public void expansionStart (final Expansion e, final boolean first)
  {
    if (!first)
      ostr.print ("\n\t\t|\t");
  }

  public void expansionEnd (final Expansion e, final boolean first)
  {}

  public void nonTerminalStart (final NonTerminal nt)
  {}

  public void nonTerminalEnd (final NonTerminal nt)
  {}

  public void reStart (final RegularExpression r)
  {}

  public void reEnd (final RegularExpression r)
  {}

  /**
   * Create an output stream for the generated Jack code. Try to open a file
   * based on the name of the parser, but if that fails use the standard output
   * stream.
   */
  protected PrintWriter create_output_stream ()
  {
    if (JJDocOptions.getOutputFile ().equals (""))
    {
      if (JJDocGlobals.input_file.equals ("standard input"))
      {
        return new PrintWriter (new OutputStreamWriter (System.out));
      }
      String ext = ".html";

      if (JJDocOptions.isText ())
      {
        ext = ".txt";
      }
      else
        if (JJDocOptions.isXText ())
        {
          ext = ".xtext";
        }

      final int i = JJDocGlobals.input_file.lastIndexOf ('.');
      if (i == -1)
      {
        JJDocGlobals.output_file = JJDocGlobals.input_file + ext;
      }
      else
      {
        final String suffix = JJDocGlobals.input_file.substring (i);
        if (suffix.equals (ext))
        {
          JJDocGlobals.output_file = JJDocGlobals.input_file + ext;
        }
        else
        {
          JJDocGlobals.output_file = JJDocGlobals.input_file.substring (0, i) + ext;
        }
      }
    }
    else
    {
      JJDocGlobals.output_file = JJDocOptions.getOutputFile ();
    }

    try
    {
      ostr = new PrintWriter (new FileWriter (JJDocGlobals.output_file));
    }
    catch (final IOException e)
    {
      error ("JJDoc: can't open output stream on file " + JJDocGlobals.output_file + ".  Using standard output.");
      ostr = new PrintWriter (new OutputStreamWriter (System.out));
    }

    return ostr;
  }

  public void debug (final String message)
  {
    System.err.println (message);
  }

  public void info (final String message)
  {
    System.err.println (message);
  }

  public void warn (final String message)
  {
    System.err.println (message);
  }

  public void error (final String message)
  {
    System.err.println (message);
  }
}
