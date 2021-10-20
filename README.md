# Processing-Tetris
Open Source Tetris built into Processing 3.5.4 - Java.<br>
Follows standard <a href="https://tetris.fandom.com/wiki/Tetris_Guideline">Tetris Guidelines</a> for endless marathon mode:
<ul>
  <li>SRS rotation with proper kick tables</li>
  <li>Trick scoring (with level scaling and back-to-back chains)</li>
  <li>3-corner T Spin / mini T Spin detection</li>
  <li>Modern DAS/ARR input system</li>
  <li>Lock delay with capped Infinity</li>
  <li>Tetris Random Generator algorithm (Grab bag randomization)</li>
  <li>Etc...</li>
</ul>

# How to Install
<ol>
  <li>Install <a href="https://processing.org/download">Processing 3.5.4</a></li>
  <li>Open and run Tetris.pde</li>
  <li>Enjoy!</li>
</ol>

# Default Controls
I don't like these controls... but they follow official Tetris Guidelines.<br>
These should be easy to modify by changing the button() bool methods:
<ul>
  <li>Left / Right = Move</li>
  <li>Z / X = Rotate</li>
  <li>C = Hold</li>
  <li>Down = Soft Drop</li>
  <li>Space = Hard Drop</li>
  <li>P = Pause</li>
</ul>
