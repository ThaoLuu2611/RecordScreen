#ifndef __TRAINING__
#define __TRAINING__


#include <conio.h>
#include <stdlib.h>
#include <stdio.h>
#include <memory.h>
#include <time.h>
#include <string>
using namespace std;



char  GetChar(void);
void  PutChar(char c);

int   GetInt(void);
void  PutInt(int num);
void  PutStr(const char* str);
int   RandNum(bool flagChange = true);


char GetChar(void)
{
    return (char)_getch();
}

void PutChar(char c)
{
    _putch(c);
}

void PutStr(const char* str)
{
	while (*str) _putch(*(str++));
}

int GetInt(void)
{
    char inputStr[20];
    unsigned idx;

    memset(inputStr, '\0', sizeof(char) * 20);

    idx = 0;
    while(1)
    {
        inputStr[idx] = _getch();

        if (inputStr[idx] == 8)
        {
            if (idx == 0)
            {
                continue;
            }
            idx--;
            _putch('\b');
            _putch(' ');
            _putch('\b');
            continue;
        }

        if (inputStr[idx] == 13) // enter
        {
            inputStr[idx] = '\0';
            _putch('\n');
            break;
        }
        _putch(inputStr[idx]);

        idx++;
    }
    
    return atoi(inputStr);
}




void PutInt(int num)
{
	char     str[16];
	int strn = 0;

	for (int n = num; strn < 16; n /= 10, strn++)
	{
		str[strn] = '0' + (n % 10);
		if (n < 10) break;
	}

	for (int c = 0; c <= strn; c++)
	{
		_putch(str[strn - c]);
	}

	return;
}


int RandNum(bool flagChange)
{
	if (flagChange == true)
	{
		static bool flag = false;
		if (flag == false)
		{
			flag = true;
			srand((unsigned int)time(0));
		}
	}
	return rand();
}



#ifdef UNICODE
#undef UNICODE
#endif

#include "windows.h"

void ClearScreen()
{
	HANDLE                     hStdOut;
	CONSOLE_SCREEN_BUFFER_INFO csbi;
	DWORD                      count;
	DWORD                      cellCount;
	COORD                      homeCoords = { 0, 0 };

	hStdOut = GetStdHandle( STD_OUTPUT_HANDLE );
	if (hStdOut == INVALID_HANDLE_VALUE) return;

	/* Get the number of cells in the current buffer */
	if (!GetConsoleScreenBufferInfo( hStdOut, &csbi )) return;
	cellCount = csbi.dwSize.X *csbi.dwSize.Y;

	/* Fill the entire buffer with spaces */
	if (!FillConsoleOutputCharacter(
		hStdOut,
		(TCHAR) ' ',
		cellCount,
		homeCoords,
		&count
		)) return;

	/* Fill the entire buffer with the current colors and attributes */
	if (!FillConsoleOutputAttribute(
		hStdOut,
		csbi.wAttributes,
		cellCount,
		homeCoords,
		&count
		)) return;

	/* Move the cursor home */
	SetConsoleCursorPosition( hStdOut, homeCoords );
}

void GotoXY(int x, int y)
{
	COORD Pos = {x - 1, y - 1};
	SetConsoleCursorPosition(GetStdHandle(STD_OUTPUT_HANDLE), Pos);
}

void SetColor(bool FR, bool FG, bool FB, bool BR, bool BG, bool BB)
{
	int c = 0;
	c |= (FB?(1<<0):0);
	c |= (FG?(1<<1):0);
	c |= (FR?(1<<2):0);
	c |= (1 ?(1<<3):0);

	c |= (BB?(1<<4):0);
	c |= (BG?(1<<5):0);
	c |= (BR?(1<<6):0);
	c |= (0 ?(1<<7):0);

	HANDLE hstdout = GetStdHandle( STD_OUTPUT_HANDLE );
	SetConsoleTextAttribute( hstdout, c );
}


namespace VGS
{

#define INVALID                   (-1)
#define SVASSERT(exp)             (exp)
#define SVMUST_TRUE(exp)          (exp)
#define KEY_QUEUE_COUNT           (19999)
#define MAX_INX_DIFF              (9999)

#define STC_WIDTH                 (320)
#define STC_HEIGHT                (200)
#define VGS_SHARED_MEMORY         "VGS"
#define VGS_SHARED_MEMORY_KEY     "VGS_KEY"
#define VGS_EVENT_KEY             "VGS_EVENT_KEY"
#define VGS_EVENT_UPDATE_B        "VGS_EVENT_UPDATE_BEGIN"
#define VGS_EVENT_UPDATE_E        "VGS_EVENT_UPDATE_END"



class STCLauncher
{
public:
    STCLauncher()
    {
        string stc_path = "";
        m_RegistryRead(stc_path);
        if(stc_path != "")
        {
            HWND win = ::FindWindow(NULL, "STC");
            if (win == NULL)
            {
                WinExec(stc_path.c_str(), SW_SHOW);
            }
        }
    }
private:
    bool m_RegistryRead(string& out_value)
    {
        HKEY hKey;
        string dir;
        string key_name;

        char szType[1024] = {0, };
        DWORD dwBufLen=1024;

        LONG lRet; 
        lRet = RegOpenKeyEx(HKEY_CURRENT_USER, "Software\\STC\\", 0, KEY_QUERY_VALUE, &hKey);

        if (lRet != ERROR_SUCCESS)
        {
            return false;
        }

        lRet = RegQueryValueEx(hKey, "path", NULL, NULL, (LPBYTE)&szType, &dwBufLen);
        if ((lRet != ERROR_SUCCESS) || (dwBufLen >= 1024))
        {
            RegCloseKey(hKey);
            return false;
        }

        RegCloseKey(hKey);
        szType[dwBufLen] = 0;
        out_value = szType;
        return true;
    }
};

class SvEvent
{
private:
	unsigned int m_handle;

public:
	SvEvent(const char* name) 
	{
		m_handle=(unsigned int)INVALID;
		Create(name);
	}
	virtual ~SvEvent(void) {SVASSERT(FlagCreate()==false);}
	bool Create(const char* globalName = NULL)
	{
		SVASSERT(FlagCreate()==false);

		m_handle = (unsigned int)::CreateEvent(NULL, FALSE, FALSE, globalName);
		if (GetLastError()==ERROR_ALREADY_EXISTS)
		{
			m_handle = (unsigned int)::OpenEvent(EVENT_ALL_ACCESS,FALSE,globalName);
		}
		if (m_handle==INVALID)
		{
			m_handle = (unsigned int)INVALID;
			return false;
		}

		::ResetEvent((HANDLE)m_handle);
		return true;
	}
	void Destroy(void)
	{
		SVASSERT(FlagCreate()==true);
		Signal();
		::CloseHandle((HANDLE)m_handle);
		m_handle = (unsigned int)INVALID;
	}
	bool FlagCreate(void) {return m_handle!=INVALID;}
	bool Signal(void)
	{
		SVASSERT(FlagCreate()==true);
		return ::SetEvent((HANDLE)m_handle) ? true : false;
	}
	void Wait(void)
	{
		SVASSERT(FlagCreate()==true);
		::WaitForSingleObject((HANDLE)m_handle, INFINITE);
	}
	void Wait(unsigned long msec)
	{
		SVASSERT(FlagCreate()==true);
		WaitForSingleObject((HANDLE)m_handle, msec);
	}
};

struct stSharedMemory
{
	char name[256];
	HANDLE handle;
	unsigned long size;
	unsigned char* pSharedMemory;
};
#define M ((stSharedMemory*)m)

class SvSharedMemory
{
private:
	void* m;

public:
	SvSharedMemory(const char* name, unsigned long size, bool bFillZero)
	{
		m = NULL;
		Create(name, size, bFillZero);
	}
	~SvSharedMemory()
	{
		Sleep(70);
		Destroy();
	}
	bool Create(const char* name, unsigned long size, bool bFillZero)
	{
		if (FlagCreate() == true) 
			return false;

		if (name==NULL)
			return false;

		m = new stSharedMemory;
		memset(m, 0, sizeof(stSharedMemory));
		M->handle        = (HANDLE)INVALID;
		M->size          = 0;
		M->pSharedMemory = NULL;

		HANDLE hMapFile = (HANDLE)INVALID;
		{
			hMapFile = CreateFileMapping((HANDLE)0xffffffff,NULL,PAGE_READWRITE,0,size,name);
			if (GetLastError()==ERROR_ALREADY_EXISTS)
			{
				hMapFile = OpenFileMapping(FILE_MAP_ALL_ACCESS, FALSE, name);

				if (hMapFile==NULL)
				{
					delete m;
					m = NULL;
					return false;
				}

				M->pSharedMemory = 
					(unsigned char*)MapViewOfFile(hMapFile, FILE_MAP_ALL_ACCESS, 0, 0, size);

			}
			if (hMapFile == 0)
			{
				return false;
			}
			M->pSharedMemory = (unsigned char*)MapViewOfFile(hMapFile,FILE_MAP_ALL_ACCESS,0,0,size);
			if (M->pSharedMemory == NULL)
				return false;

			if(bFillZero)
				memset(M->pSharedMemory, 0, size);
		}

		strcpy_s(M->name, name);
		M->handle = hMapFile;
		M->size = size;
		return true;
	}
	void Destroy(void)
	{
		if (FlagCreate()== false) return;

		if (M->handle!=(HANDLE)INVALID)
		{
			//UnmapViewOfFile( M->pSharedMemory );
			SVMUST_TRUE(CloseHandle(M->handle) ? true : false);
		}
		delete m;
		m = NULL;

	}
	bool FlagCreate(void) {return m!=NULL?true:false;}

	bool Read(unsigned char* out, unsigned long size = INVALID)
	{
		SVASSERT(FlagCreate());

		if (size==INVALID)
			size = M->size;

		memcpy(out, M->pSharedMemory, size);
		return true;

	}
	bool Read(unsigned char* out, unsigned long offset, unsigned long size)
	{
		SVASSERT(FlagCreate());

		if (size==INVALID)
			size = M->size;

		memcpy(out, M->pSharedMemory + offset, size);
		return true;

	}
	bool Write(void* in, unsigned long size = INVALID)
	{
		SVASSERT(FlagCreate());

		if (size==INVALID)
			size = M->size;

		memcpy(M->pSharedMemory, in, size);
		return true;
	}
	bool Write(void* in, unsigned long offset, unsigned long size)
	{
		SVASSERT(FlagCreate());

		if (size==INVALID)
			size = M->size;

		memcpy(M->pSharedMemory + offset, in, size);
		return true;
	}
};



static STCLauncher launcher;
static unsigned int* buf  = new unsigned int[STC_WIDTH * STC_HEIGHT + 2]();
static SvSharedMemory sm (VGS_SHARED_MEMORY, (STC_WIDTH * STC_HEIGHT + 2) * 4, true);
static SvSharedMemory smk(VGS_SHARED_MEMORY_KEY, (KEY_QUEUE_COUNT+1)*4, false);
static unsigned int   lkc = INT_MAX;
static SvEvent evk(VGS_EVENT_KEY);
static SvEvent evb(VGS_EVENT_UPDATE_B);
static SvEvent eve(VGS_EVENT_UPDATE_E);

}//namespace STC


static void SetPixel(unsigned int posX, unsigned int posY, unsigned int color) 
{
	if(posX>=STC_WIDTH || posY>=STC_HEIGHT) return;

	VGS::buf[posY*STC_WIDTH+posX] = color;
	VGS::sm.Write(&color, (posY*STC_WIDTH+posX)*4, 4);
}




static unsigned int GetPixel(unsigned int posX, unsigned int posY)
{
	if(posX>=STC_WIDTH || posY>=STC_HEIGHT) return 0x00000000;

	return VGS::buf[posY*STC_WIDTH+posX];
}



#pragma comment(lib, "winmm.lib")

long GetMilisec()
{
    static int d = 0;
    static LARGE_INTEGER init;
    if (d == 0)
    {
        d = 1;
        QueryPerformanceFrequency(&init);
     }
     LARGE_INTEGER c;
     QueryPerformanceCounter(&c);
     return (long)(c.QuadPart * 1000 / init.QuadPart);//clock();
}

static bool GetInputState(int keycode)
{
    HWND h1 = ::FindWindow(NULL, "STC");
    HWND h2 = ::GetForegroundWindow();
    if (h1 != h2)
    {
        return false;
    }

    int ms_keycode = -1;

    if (keycode < 128) ms_keycode = keycode;
    if (keycode>='a' && keycode<='z') ms_keycode = keycode - ('A'-'a');

    switch(keycode)
    {
            case 128 : ms_keycode = VK_F1     ;break;
            case 129 : ms_keycode = VK_F2     ;break;
            case 130 : ms_keycode = VK_F3     ;break;
            case 131 : ms_keycode = VK_F4     ;break;
            case 132 : ms_keycode = VK_F5     ;break;
            case 133 : ms_keycode = VK_F6     ;break;
            case 134 : ms_keycode = VK_F7     ;break;
            case 135 : ms_keycode = VK_F8     ;break;
            case 136 : ms_keycode = VK_F9     ;break;
            case 137 : ms_keycode = VK_F10    ;break;
            case 138 : ms_keycode = VK_F11    ;break;
            case 139 : ms_keycode = VK_F12    ;break;
            case 140 : ms_keycode = VK_PRIOR  ;break;
            case 141 : ms_keycode = VK_NEXT   ;break;
            case 142 : ms_keycode = VK_END    ;break;
            case 143 : ms_keycode = VK_HOME   ;break;
            case 144 : ms_keycode = VK_LEFT   ;break;
            case 145 : ms_keycode = VK_UP     ;break;
            case 146 : ms_keycode = VK_RIGHT  ;break;
            case 147 : ms_keycode = VK_DOWN   ;break;
            case 148 : ms_keycode = VK_INSERT ;break;
            case 149 : ms_keycode = VK_DELETE ;break;
            case 150 : ms_keycode = VK_SHIFT  ;break;
            case 151 : ms_keycode = VK_CONTROL;break;
    }

    return (GetAsyncKeyState(ms_keycode)	& 0x8000) ? true : false;
}



static int GetInput(bool wait = false)
{
	int ret = -1;

	if (wait) 
		VGS::evk.Wait();

	if (VGS::smk.FlagCreate() == false)
		VGS::smk.Create(VGS_SHARED_MEMORY_KEY, (KEY_QUEUE_COUNT+1)*4, false);

	if (VGS::smk.FlagCreate() == false)
		return -1;

	unsigned int skc = 0;
	VGS::smk.Read((unsigned char*)&skc, 0, 4);

	if (VGS::lkc > skc)
		VGS::lkc = skc;

	int ski = (  skc%KEY_QUEUE_COUNT) + 1;
	int lki = (VGS::lkc%KEY_QUEUE_COUNT) + 1;

	if (ski == lki)
	{
		if(wait) 
			VGS::lkc--, lki = (VGS::lkc%KEY_QUEUE_COUNT) + 1;
		else 
			return -1;
	}

	if (skc - VGS::lkc > MAX_INX_DIFF)
		VGS::lkc = ((int)skc-MAX_INX_DIFF)<0 ? 0 : ((int)skc-MAX_INX_DIFF),
		lki = (VGS::lkc%KEY_QUEUE_COUNT)+1;

	ret = -1;
	VGS::smk.Read((unsigned char*)&ret, lki*4, 4);
	VGS::lkc++;
	return ret;

}


static void BeginUpdate(void)
{
	VGS::buf[STC_WIDTH * STC_HEIGHT] = 1;
	VGS::sm.Write(&VGS::buf[STC_WIDTH * STC_HEIGHT], (STC_WIDTH * STC_HEIGHT)*4, 4);
}




static void EndUpdate(void)
{
	VGS::evb.Signal();
	VGS::eve.Wait();
	VGS::buf[STC_WIDTH * STC_HEIGHT] = 0;
	VGS::sm.Write(&VGS::buf[STC_WIDTH * STC_HEIGHT], (STC_WIDTH * STC_HEIGHT)*4, 4);
}




#endif