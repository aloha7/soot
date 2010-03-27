/*
 * Copyright (c) 2001, University of Washington, Department of
 * Computer Science and Engineering.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither name of the University of Washington, Department of
 * Computer Science and Engineering nor the names of its contributors
 * may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package one.radio;

import one.world.core.Tuple;
import one.world.core.TupleException;
import one.world.core.InvalidTupleException;

import javax.sound.sampled.AudioFormat;


/**
 * A tuple representing an audio format.
 *
 * @see      javax.sound.sampled.AudioFormat
 *
 * @version  $Revision: 1.3 $
 * @author   Janet Davis
 */
public final class AudioDescriptor extends Tuple {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -2565597549574716887L;

  /** Type code for the signed PCM audio encoding. */
  public static final int PCM_SIGNED = 1;

  /** Type code for the unsigned PCM audio encoding. */
  public static final int PCM_UNSIGNED = 2;

  /** Type code for the u-law audio encoding. */
  public static final int ULAW = 3;

  /** Type code for the a-law audio encoding. */
  public static final int ALAW = 4;

  /** 
   * True if the audio data is stored in big-endian order; false if it is 
   * little-endian.
   */
  public boolean bigEndian;

  /** 
   * The number of audio channels in this format (1 for mono, 2 for
   * stereo).
   */
  public int channels;

  /** 
   * The encoding for this audio format: {@link #PCM_SIGNED}, {@link
   * #PCM_UNSIGNED}, {@link #ULAW}, or {@link #ALAW}.
   *
   * @serial  Must not be <code>null</code>.
   */
  public int encoding;

  /** 
   * The number of frames played or recorded per second for audio data
   * with this format.
   */
  public float frameRate;

  /** 
   * The number of bytes in each frame of audio data with this format.
   */
  public int frameSize;
  
  /** The number of samples played or recorded per second in this format. */
  public float sampleRate;

  /** The number of bits in each sample in this format. */
  public int sampleSizeInBits;

  /** Constructs a new, empty AudioDescriptor. */
  public AudioDescriptor() {}

  /** 
   * Constructs a new AudioDescriptor with the given parameters. 
   *
   * @param encoding   The encoding for this audio format.  See
   *                   {@link #encoding}.
   * @param sampleRate The number of samples played or recorded per second
   *                   in this format.
   * @param sampleSizeInBits The number of bits in each sample in this
   *                   format.
   * @param channels   The number of channels in this format (1 for mono,
   *                   2 for stereo).
   * @param frameSize  The number of bytes in each frame of audio data
   *                   with this format.
   * @param frameRate  The number of samples played or recorded per second
   *                   in this format.
   * @param bigEndian  True if the audio data is stored in big-endian
   *                   order; false if it is little-endian.
   */
  public AudioDescriptor(int encoding, 
                         float sampleRate, int sampleSizeInBits, 
			 int channels,
			 int frameSize, float frameRate,
			 boolean bigEndian) {
    this.encoding = encoding;
    this.sampleRate = sampleRate;
    this.sampleSizeInBits = sampleSizeInBits;
    this.channels = channels;
    this.frameSize = frameSize;
    this.frameRate = frameRate;
    this.bigEndian = bigEndian;
  }
  
  /** 
   * Constructs a new AudioDescriptor from the given AudioFormat. 
   *
   * @param format   The audio format to replicate.
   */
  public AudioDescriptor(final AudioFormat format) {
    this(translateEncoding(format.getEncoding()),
         format.getSampleRate(), format.getSampleSizeInBits(),
         format.getChannels(),
	 format.getFrameSize(), format.getFrameRate(),
	 format.isBigEndian());
  }

  /**
   * Constructs a new AudioDescriptor with a linear PCM encoding and the
   * given parameters.
   *
   * @param sampleRate The number of samples played or recorded per second
   *                   in this format.
   * @param sampleSizeInBits The number of bits in each sample in this
   *                   format.
   * @param channels   The number of channels in this format (1 for mono,
   *                   2 for stereo).
   * @param signed     Indicates whether to use signed data.
   * @param bigEndian  True if the audio data is stored in big-endian
   *                   order; false if it is little-endian.
   */
  public AudioDescriptor(float sampleRate, int sampleSizeInBits, 
			  int channels,
			  boolean signed,
			  boolean bigEndian) {
    this(new AudioFormat(sampleRate, sampleSizeInBits, channels,
                         signed, bigEndian));
  }

  /** 
   * Translates an AudioFormat.Encoding object to an integer encoding type
   * code.
   *
   * @param encoding  The encoding object. 
   * @return The corresponding type code.
   */
  static int translateEncoding(AudioFormat.Encoding encoding) {
    if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
      return PCM_SIGNED;
    } else if (encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
      return PCM_UNSIGNED;
    } else if (encoding.equals(AudioFormat.Encoding.ULAW)) {
      return ULAW;
    } else if (encoding.equals(AudioFormat.Encoding.ALAW)) {
      return ALAW;
    } else {
      throw new IllegalArgumentException("Unrecognized audio format: "
                                         + encoding);
    }
  }

  /**
   * Translates an integer encoding type code into an AudioFormat.Encoding
   * object.
   *
   * @param encoding  The encoding type code.
   * @return The corresponding encoding object.
   */
  static AudioFormat.Encoding translateEncoding(int encoding) {
    switch (encoding) {
    case PCM_SIGNED:
      return AudioFormat.Encoding.PCM_SIGNED;
    case PCM_UNSIGNED:
      return AudioFormat.Encoding.PCM_UNSIGNED;
    case ULAW:
      return AudioFormat.Encoding.ULAW;
    case ALAW:
      return AudioFormat.Encoding.ALAW;
    default:
      throw new IllegalArgumentException(
                    "Unrecognized encoding type code: " + encoding);
    }
  }

  /** Constructs an AudioFormat equivalent to this AudioDescriptor. */
  public AudioFormat getAudioFormat() {
    return new AudioFormat(translateEncoding(encoding), 
                           sampleRate, sampleSizeInBits,
			   channels,
			   frameSize, frameRate,
			   bigEndian);
  }

  /** Validates this AudioDescriptor. */
  public void validate() throws TupleException {
    if (encoding < 1 || encoding > 4) {
      throw new InvalidTupleException("Invalid encoding (" + encoding +
                                      ") for audio descriptor (" + this + ")");
    }
    if (channels < 1 || channels > 2) {
      throw new InvalidTupleException("Invalid channel number (" + channels +
                                      ") for audio descriptor (" + this + ")");
    }
  }

  /** Returns a string representation of this AudioDescriptor. */
  public String toString() {
    StringBuffer buffer = new StringBuffer();

    buffer.append("[# audio format: ");
    buffer.append(translateEncoding(encoding).toString());
    buffer.append(", ");
    buffer.append(sampleRate);
    buffer.append(" Hz, ");
    buffer.append(sampleSizeInBits);
    buffer.append(" bit, ");

    switch (channels) {
    case 1: 
      buffer.append("mono, ");
      break;
    case 2:
      buffer.append("stereo, ");
      break;
    }

    if (bigEndian) {
      buffer.append("big-endian, ");
    } else {
      buffer.append("little-endian, ");
    }

    buffer.append("audio data]");

    return buffer.toString();
  }
}
