// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input. 
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel. When no key is pressed, the
// program clears the screen, i.e. writes "white" in every pixel.


(INFINITE)

// if keyboard not pressed: go back to the beg
@KBD
D=M
@INFINITE 
D;JEQ




(BLACK)

// R[1] stores the maximum size of screen memory
@8192	
D=A
@1
M=D

(BLACKLOOP)


// D= @SCREEN + R[1]
@SCREEN
D=A
@1
A=M+D 		

// Set to -1 (all black)
M=-1

// R[1]--
@1
M=M-1

// if R[1] is negative go back to infinite 
D=M
@INFINITE
D;JLT

//if(keyboardNotPressed) jump to white
@KBD
D=M
@WHITE
D;JEQ

@BLACKLOOP
0;JEQ

// ------------------------------------
(WHITE)

// R[1] stores the maximum size of screen memory
@8192	
D=A
@1
M=D

// possibly deletable
@SCREEN
D=A
@0
M=D
// 

(WHITELOOP)

// D= @SCREEN + R[1]
@SCREEN
D=A
@1
A=M+D 	

// Set to 0 (all white)
M=0

// R[1]--
@1
M=M-1

// if R[1] is negative go back to infinite 
D=M
@INFINITE
D;JLT

@WHITELOOP
0;JEQ
// ------------------------------------


@INFINITE
0;JEQ