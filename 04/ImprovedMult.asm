@0
D=M
@3
M=D
@1
D=M
@3
M=M-D
D=M
@17
D;JGT
@3
D=M
@0
M=M-D
@1
M=D+M


// R2 = 0
@2
M=0
// Set the smaller one to R1 and the bigger to R0
// 
@3
M=0

// R3 = R1 (will be used as counter)
@1
D=M
@3
M=D
//--------------------------------------

@2
D=M
@0
D=D+M

@2
M=D


// R3-- 
@3
D=M
M=D-1;

@29
D-1;JEQ

@14
0;JEQ

// end of program
@38
0;JMP