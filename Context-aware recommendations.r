#Context-aware recommendations using R
#laod data
raw_data = read.csv("u.data",sep="\t",header=F)
colnames(raw_data) = c("UserId","MovieId","Rating","TimeStamp")
head(raw_data)

movies = read.csv("u.item",sep="|",header=F)
colnames(movies) = c("MovieId","MovieTitle","ReleaseDate","VideoReleaseDate","IMDbURL","Unknown",
                     "Action","Adventure","Animation","Children","Comedy","Crime","Documentary",
                     "Drama","Fantasy","FilmNoir","Horror","Musical","Mystery","Romance","SciFi",
                     "Thriller","War","Western")

#remove unwanted columns from the data frame
movies = movies[,-c(2:5)]

#merge the Movies and Ratings datasets
ratings_ctx = merge(x = raw_data, y = movies, by = "MovieId", all.x = TRUE)

# the context is the hour of the day

# create context profile

ts = ratings_ctx$TimeStamp

# convert it into a POSIXlt date object and using hour property to extract hour of the day:
hours <- as.POSIXlt(ts,origin="1960-10-01")$hour

#can append the hours back on to the ratings dataset
ratings_ctx = data.frame(cbind(ratings_ctx,hours))

# building a context profile for a user with the user ID 943:

# Extract ratings information for the active user(943)
# removing UserId, MovieId, Rating,Timestamp columns

UCP = ratings_ctx[(ratings_ctx$UserId == 943),][,-c(2,3,4,5)]

# compute the columns of all the item features. This columnwise sum is used
# to compute the preferences for the item features for each hour of the day

UCP_pref = aggregate(.~hours,UCP[,-1],sum)

#normalize the preceding data between 0-1
UCP_pref_sc = cbind(context = UCP_pref[,1],t(apply(UCP_pref[,-1], 1,
                                                   function(x)(x-min(x))/(max(x)-min(x)))))


# Generating context-aware recommendations
#reuse the recommend object built using R, which contains content recommendations for all the users

recommend$MovieId

# add our time or hour of the day dimension and then generate recommendations as per the current context

# merge recommendations and movies dataset
UCP_pref_content = merge(x = recommend, y = movies, by = "MovieId", all.x = TRUE)

# required matrices, user context profile (UCP_Pref_SC) and user content recommendations (UCP_Pref_content)
# now computed

# generate recommendations for the user at the ninth hour

# Performing element wise multiplication for the User content recommendations and the ninth
# hour context preferences for the user

active_user =cbind(UCP_pref_content$MovieId,
                   (as.matrix(UCP_pref_content[,-c(1,2,3)]) %*% as.matrix(UCP_pref_sc[4,2:19])))

# create a dataframe object of the prediction object
active_user_df = as.data.frame(active_user)

#add column names to the predictions object
names(active_user_df) = c('MovieId','SimVal')

# sort the results
FinalPredicitons_943 = active_user_df[order(-active_user_df$SimVal),]