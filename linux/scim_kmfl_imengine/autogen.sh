#! /bin/sh
# bootstrap -- Use this script to create generated files from the CVS dist
# Copyright (C) 2000 Gary V. Vaughan
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2, or (at your option)
# any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

## @start 1
#! /bin/sh

set -x
aclocal -I m4
autoheader
libtoolize -c --automake 
intltoolize -c --automake
automake --add-missing --copy --include-deps
autoconf
## @end 1
