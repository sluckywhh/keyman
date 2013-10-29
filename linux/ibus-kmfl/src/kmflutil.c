/* vim:set et sts=4: */
#include <stdlib.h>
#include <dirent.h>
#include <sys/stat.h>
#include <glib.h>
#include <glib/gprintf.h>
#include <string.h>

#include "kmflutil.h"

#define N_(text) text
static gchar * get_dirname(const gchar * path)
{
    gchar * dirend = g_strrstr(path, "/");

    if (dirend) {
        return g_strndup(path, dirend-path);
    } else {
        return g_strdup("");
    }
}

GList * kmfl_get_keyboard_list( const gchar * path)
{
    GList * keyboard_list=NULL;

    DIR *dir = opendir(path);

    if (dir != NULL) {
        struct dirent *file = readdir(dir);
        while (file != NULL) {
            struct stat filestat;
            gchar * absfn = g_strdup_printf("%s/%s", path, file->d_name);
            stat(absfn, &filestat);


            // Only .kmfl and .kmn extensions are valid keyboard files
            if (S_ISREG(filestat.st_mode)
                && ((g_str_has_suffix(absfn, ".kmfl") && kmfl_check_keyboard(absfn) == 0) 
                || g_str_has_suffix(absfn, ".kmn"))) {

                keyboard_list=g_list_append(keyboard_list, absfn);
            }

            file = readdir(dir);
        }
        closedir(dir);
    }
    return keyboard_list;
}

gchar * kmfl_get_icon_file(KInputMethod * im)
{
    const char * icon_file = kmfl_icon_file(im->keyboard_number);
    gchar * full_path_to_icon_file=NULL;
    struct stat filestat;
    char * valid_extensions[]= {"", ".png", ".bmp", ".jpg", ".ico", NULL};
    int valid_extension_index;
            
    if (strlen(icon_file) > 0) {
        for (valid_extension_index=0; valid_extensions[valid_extension_index] != NULL; valid_extension_index++) {
            full_path_to_icon_file=g_strdup_printf("%s/icons/%s%s", get_dirname(im->keyboard_filename), icon_file, valid_extensions[valid_extension_index]);

            stat(full_path_to_icon_file, &filestat);

            if (S_ISREG(filestat.st_mode)) {
                break;
            } else {
                g_free(full_path_to_icon_file);
                full_path_to_icon_file=NULL;
            }
        }
    }

    if (full_path_to_icon_file==NULL)
         full_path_to_icon_file=g_strdup("/usr/share/kmfl/icons/default.png");

    return full_path_to_icon_file;
}

void kmfl_get_keyboard_info(KInputMethod * im) 
{
    char buf[1024];
    KMSI * p_kmsi;
    
    im->keyboard_name = g_strdup(kmfl_keyboard_name(im->keyboard_number));
    p_kmsi = kmfl_make_keyboard_instance(NULL);
    kmfl_attach_keyboard(p_kmsi, im->keyboard_number);
    
    *buf='\0';
    kmfl_get_header(p_kmsi,SS_AUTHOR,buf,sizeof(buf) - 1);
    im->keyboard_author=g_strdup(buf);
    
    *buf='\0';
    kmfl_get_header(p_kmsi,SS_COPYRIGHT,buf,sizeof(buf) - 1);
    im->keyboard_copyright=g_strdup(buf);
    
    *buf='\0';
    kmfl_get_header(p_kmsi,SS_LANGUAGE,buf,sizeof(buf) - 1);
    im->keyboard_language=g_strdup(buf);
    
    *buf='\0';
    kmfl_get_header(p_kmsi,SS_MESSAGE,buf,sizeof(buf) - 1);
    im->keyboard_description=g_strdup(buf);

    if (g_strrstr(buf, "license") ||  g_strrstr(buf, "License") || g_strrstr(buf, "LICENSE"))
        im->keyboard_license=g_strdup(buf);
    else
        im->keyboard_license=g_strdup("");

    *buf='\0';
    kmfl_get_header(p_kmsi,SS_LAYOUT,buf,sizeof(buf) - 1);
    if (*buf != '\0')    
        im->keyboard_layout=g_strdup(buf);
    else
        im->keyboard_layout=g_strdup("us");
        
    kmfl_detach_keyboard(p_kmsi);
    kmfl_delete_keyboard_instance(p_kmsi);
    im->keyboard_icon_filename = kmfl_get_icon_file(im);
}

void kmfl_free_keyboard_info(KInputMethod * im)
{
    g_assert(im != NULL);  
    g_free(im->keyboard_filename);  
    g_free(im->keyboard_name);
    g_free(im->keyboard_author);
    g_free(im->keyboard_copyright);
    g_free(im->keyboard_language);
    g_free(im->keyboard_description);
    g_free(im->keyboard_icon_filename);
    g_free(im->keyboard_layout);
    g_free(im->keyboard_license);
}


static IBusEngineDesc *
ibus_kmfl_engine_new (gchar * file_name,
                      gchar *lang,
                      gchar *name,
                      gchar *author,
                      gchar *icon,
                      gchar *copyright,
                      gchar *description,
                      gchar *layout,
                      gchar *license)
{
    IBusEngineDesc *engine;
    gchar * desc = g_strdup_printf("%s\n%s", description, copyright);
    
    engine = ibus_engine_desc_new (file_name,
                                   name,
                                   desc ? desc : "",
                                   lang,
                                   license ? license : "",
                                   author ? author : "",
                                   icon ? icon : "",
                                   layout);

    return engine;
}

GList * 
ibus_kmfl_add_engines(GList * engines, GList * keyboard_list)
{
    GList *p;
    for (p=keyboard_list; p != NULL; p = p->next) {
        gchar * keyboard_filename = (gchar *) p->data;
        KInputMethod im;
        
        im.keyboard_number = kmfl_load_keyboard(keyboard_filename);
        if (im.keyboard_number >=0) {
            im.keyboard_filename = g_strdup(keyboard_filename);

            kmfl_get_keyboard_info(&im);
            engines = g_list_append (engines, ibus_kmfl_engine_new (keyboard_filename, im.keyboard_language, im.keyboard_name, im.keyboard_author, im.keyboard_icon_filename, im.keyboard_copyright, im.keyboard_description, im.keyboard_layout, im.keyboard_license));
            g_free(p->data);
            kmfl_free_keyboard_info(&im);
        }
    }
    return engines;
}

GList *
ibus_kmfl_list_engines (void)
{
    GList *engines = NULL;
    GList *keyboard_list;
    gchar *local_keyboard_path;

    keyboard_list = kmfl_get_keyboard_list("/usr/share/kmfl");

    engines = ibus_kmfl_add_engines(engines, keyboard_list);

    g_list_free(keyboard_list);

    local_keyboard_path= g_strdup_printf("%s/.kmfl", getenv("HOME"));

    keyboard_list = kmfl_get_keyboard_list(local_keyboard_path);

    engines = ibus_kmfl_add_engines(engines, keyboard_list);

    g_free(local_keyboard_path);

    g_list_free(keyboard_list);
    
    return engines;
}

IBusComponent *
ibus_kmfl_get_component (void)
{
    GList *engines, *p;
    IBusComponent *component;

    component = ibus_component_new ("org.freedesktop.IBus.KMFL",
                                    N_("KMFL"),
                                    "0.1.0",
                                    "GPL",
                                    "Doug Rintoul <doug_rintoul@sil.org",
                                    "http://kmfl.sourceforge.org",
                                    "",
                                    "ibus-kmfl");

    engines = ibus_kmfl_list_engines ();

    for (p = engines; p != NULL; p = p->next) {
        ibus_component_add_engine (component, (IBusEngineDesc *) p->data);
    }

    g_list_free (engines);
    return component;
}

KInputMethod * kinput_open_im(const gchar * keyboard_filename)
{
    KInputMethod * im = g_new(KInputMethod, 1);
    
    if (im != NULL) {
        im->keyboard_number = kmfl_load_keyboard(keyboard_filename);
        if (im->keyboard_number < 0) {
            g_free(im);
            im = NULL;
        } else {
            im->keyboard_filename = g_strdup(keyboard_filename);
            kmfl_get_keyboard_info(im) ;
        }
    }
    return im;
}

void kinput_close_im(KInputMethod * im)
{
    g_assert(im != NULL);
    kmfl_free_keyboard_info(im);
    kmfl_unload_keyboard(im->keyboard_number);
    g_free(im);
}

#ifdef DEBUG
#include <locale.h>

int main ()
{
    IBusComponent *component;
    GString *output;

    setlocale (LC_ALL, "");
    ibus_init ();

    component = ibus_kmfl_get_component ();

    output = g_string_new ("");

    ibus_component_output (component, output, 1);

    g_debug ("\n%s", output->str);

    g_string_free (output, TRUE);
    g_object_unref (component);

    return 0;
}
#endif

